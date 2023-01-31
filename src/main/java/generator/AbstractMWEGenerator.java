package generator;

import fragment.ICodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import utility.CollectionsUtility;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractMWEGenerator {

	protected final TestExecutorOptions m_testExecutorOptions;

	public AbstractMWEGenerator(TestExecutorOptions options) {
		m_testExecutorOptions = options;
	}

	public void runGenerator() {
		// extract code fragments
		ITestExecutor executor = getTestExecutor();
		executor.initialize();
		List<ICodeFragment> fragments;
		int testNr = 1;
		int totalFragments;
		do {
			logInfo("############## RUNNING TEST NR. " + testNr++ + " ##############");
			fragments = executor.extractFragments();
			totalFragments = fragments.size();
			long start = System.currentTimeMillis();
			fragments = runDDMin(executor, fragments, totalFragments);
			long time = System.currentTimeMillis() - start;
			logInfo(null);
			logInfo("Found a 1-minimal configuration in " + time + " ms:");
			logInfo(getConfigurationIdentifier(fragments, totalFragments));

			// recreate mwe
			logInfo("Recreating result in testingoutput folder...");
			executor.recreateCode(fragments);
			executor.changeSourceToOutputFolder();
		} while (m_testExecutorOptions.isMultipleRuns() && fragments.size() < totalFragments);

		logInfo("############## FINISHED ##############");
	}

	protected List<ICodeFragment> runDDMin(ITestExecutor executor, List<ICodeFragment> initialConfiguration, int totalFragments) {
		Map<String, ITestExecutor.ETestResult> resultMap = new HashMap<>();

		checkPreconditions(executor, initialConfiguration, totalFragments, resultMap);

		List<ICodeFragment> fragments = new ArrayList<>(initialConfiguration);
		int granularity = 2;
		while (fragments.size() >= 2) {
			List<List<ICodeFragment>> subsets = CollectionsUtility.split(fragments, granularity);
			assert subsets.size() == granularity;

			boolean someComplementIsFailing = false;
			for (List<ICodeFragment> subset : subsets) {
				List<ICodeFragment> complement = CollectionsUtility.listMinus(fragments, subset);

				if (executeTest(executor, complement, totalFragments, resultMap) == ITestExecutor.ETestResult.FAILED) {
					fragments = complement;
					granularity = Math.max(granularity - 1, 2);
					someComplementIsFailing = true;
					break;
				}
			}

			if (!someComplementIsFailing) {
				if (granularity == fragments.size()) {
					break;
				}

				granularity = Math.min(granularity * 2, fragments.size());
			}
		}
		return fragments;
	}

	protected void checkPreconditions(ITestExecutor executor, List<ICodeFragment> initialConfiguration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
		if (executeTest(executor, Collections.emptyList(), totalFragments, resultMap) == ITestExecutor.ETestResult.FAILED
				|| executeTest(executor, initialConfiguration, totalFragments, resultMap) != ITestExecutor.ETestResult.FAILED) {
			logInfo("Initial testing conditions are not met.");
			System.exit(1);
		}
	}

	protected ITestExecutor.ETestResult executeTest(ITestExecutor executor, List<ICodeFragment> configuration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
		String configurationIdentifier = getConfigurationIdentifier(configuration, totalFragments);
		ITestExecutor.ETestResult result = resultMap.get(configurationIdentifier);
		if (result != null) {
			return result;
		}

		result = executor.test(configuration);

		logDebug(configurationIdentifier + " -> " + result);

		resultMap.put(configurationIdentifier, result);

		return result;
	}

	protected String getConfigurationIdentifier(List<ICodeFragment> configuration, int totalFragments) {
		List<Integer> activeFragments = IntStream.range(0, totalFragments)
				.mapToObj(i -> 0)
				.collect(Collectors.toList());

		configuration.forEach(fr -> activeFragments.set(fr.getFragmentNumber(), 1));

		return activeFragments.stream().map(i -> Integer.toString(i)).collect(Collectors.joining());
	}

	protected abstract ITestExecutor getTestExecutor();

	protected void logInfo(Object msg) {
		log(msg, TestExecutorOptions.ELogLevel.INFO);
	}

	protected void logDebug(Object msg) {
		log(msg, TestExecutorOptions.ELogLevel.DEBUG);
	}

	private void log(Object msg, TestExecutorOptions.ELogLevel level) {
		if (!m_testExecutorOptions.getLogLevel().shouldLog(level)) {
			return;
		}

		if (msg != null) {
			System.out.println(msg);
		} else {
			System.out.println();
		}
	}
}
