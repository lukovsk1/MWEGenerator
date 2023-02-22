package generator;

import fragment.ICodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import utility.CollectionsUtility;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractMWEGenerator {

	protected final TestExecutorOptions m_testExecutorOptions;
	protected final ExecutorService m_executorService;

	public AbstractMWEGenerator(TestExecutorOptions options) {
		m_testExecutorOptions = options;
		if (options.isConcurrentExecution()) {
			if (options.getCompilationType() == TestExecutorOptions.ECompilationType.COMMAND_LINE) {
				System.out.println("Concurrent execution and command line are not compatible");
				System.exit(1);
			}
			m_executorService = Executors.newFixedThreadPool(16);
		} else {
			m_executorService = null;
		}
	}

	public void runGenerator() {
		try {
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
		} finally {
			cleanup();
		}
	}

	protected void cleanup() {
		if (m_executorService != null) {
			m_executorService.shutdown();
		}
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
			List<Callable<List<ICodeFragment>>> taskList = new ArrayList<>();
			for (List<ICodeFragment> subset : subsets) {
				List<ICodeFragment> complement = CollectionsUtility.listMinus(fragments, subset);

				if (m_testExecutorOptions.isConcurrentExecution()) {
					taskList.add(() -> {
						if (executeTest(executor, complement, totalFragments, resultMap) == ITestExecutor.ETestResult.FAILED) {
							return complement;
						}
						throw new Exception("Test did not fail");
					});
				} else {
					if (executeTest(executor, complement, totalFragments, resultMap) == ITestExecutor.ETestResult.FAILED) {
						fragments = complement;
						someComplementIsFailing = true;
						break;
					}
				}
			}
			if (m_testExecutorOptions.isConcurrentExecution()) {
				try {
					fragments = m_executorService.invokeAny(taskList);
					someComplementIsFailing = true;
				} catch (ExecutionException e) {
					// no task completed successfully -> increase granularity
				} catch (InterruptedException e) {
					System.out.println("ERROR: Exception occured when running ddmin concurrently: " + e);
					System.exit(1);
				}
			}

			if (someComplementIsFailing) {
				granularity = Math.max(granularity - 1, 2);
				logInfo("DDmin: granularity decreased to " + granularity + " / " + fragments.size());
			} else {
				if (granularity == fragments.size()) {
					break;
				}

				granularity = Math.min(granularity * 2, fragments.size());
				logInfo("DDmin: granularity increased to " + granularity + " / " + fragments.size());
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

	protected void log(Object msg, TestExecutorOptions.ELogLevel level) {
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
