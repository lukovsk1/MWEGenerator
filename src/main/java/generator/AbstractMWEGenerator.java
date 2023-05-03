package generator;

import fragment.ICodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import testexecutor.TestingException;
import utility.CollectionsUtility;
import utility.StatsTracker;
import utility.StatsUtility;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public abstract class AbstractMWEGenerator {

	protected final TestExecutorOptions m_testExecutorOptions;
	protected final ExecutorService m_executorService;
	protected List<ICodeFragment> m_fragments;
	protected AtomicBoolean m_isCancelled = new AtomicBoolean();
	protected long m_levelStart;

	public AbstractMWEGenerator(TestExecutorOptions options) {
		m_testExecutorOptions = options;
		if (options.getNumberOfThreads() > 1) {
			if (options.getCompilationType() == TestExecutorOptions.ECompilationType.COMMAND_LINE) {
				System.out.println("Concurrent execution and command line are not compatible");
				System.exit(1);
			}
			m_executorService = Executors.newFixedThreadPool(options.getNumberOfThreads());
		} else {
			m_executorService = null;
		}

		StatsUtility.getStatsTracker().writeRunConfiguration(this.getClass().getSimpleName(), options);
	}

	public void runGenerator() {
		StatsTracker statsTracker = StatsUtility.getStatsTracker();
		ITestExecutor executor = getTestExecutor();
		try {
			int testNr = 0;
			int totalFragments = -1;
			int activeFragments;
			do {
				logInfo("############## RUNNING TEST NR. " + testNr + " ##############");
				// extract code fragments
				executor.initialize();
				m_fragments = executor.extractFragments();
				if (totalFragments < 0) {
					totalFragments = m_fragments.size();
				}
				activeFragments = m_fragments.size();
				m_levelStart = System.currentTimeMillis();
				statsTracker.startTrackingDDminExecution(String.valueOf(testNr), activeFragments, activeFragments);
				m_fragments = runDDMin(executor, m_fragments, activeFragments);
				logInfo("############## FINISHED NR. " + testNr++ + " in " + StatsUtility.formatDuration(m_levelStart) + " :::: Reduced to " + m_fragments.size() + " out of " + totalFragments + " :::: " + executor.getStatistics() + " ##############");
				executor.trackDDminCompilerStats();
				statsTracker.trackDDminExecutionEnd(m_levelStart, m_fragments.size(), m_fragments.size());

				// recreate mwe
				logInfo("Recreating result in testingoutput folder...");
				executor.recreateCode(m_fragments);
				executor.changeSourceToOutputFolder();
			} while (m_testExecutorOptions.isMultipleRuns() && m_fragments.size() < activeFragments);

			logInfo("Formatting result in testingoutput folder...");
			executor.formatOutputFolder();
			logInfo("############## FINISHED ##############");
		} catch (CancellationException e) {
			logInfo("Execution was manually cancelled. Recreate intermediate result in testingoutput folder...");
			executor.trackDDminCompilerStats();
			if (m_fragments != null && !m_fragments.isEmpty()) {
				statsTracker.trackDDminExecutionEnd(m_levelStart, m_fragments.size(), m_fragments.size());
				executor.recreateCode(m_fragments);
				executor.formatOutputFolder();
			}
			throw e;
		} finally {
			statsTracker.writeInputFragments(m_fragments.size());
			cleanup();
		}
	}

	public void cancelAndWriteIntermediateResult() {
		m_isCancelled.set(true);
	}

	protected void cleanup() {
		if (m_executorService != null) {
			m_executorService.shutdownNow();
		}
	}

	protected List<ICodeFragment> runDDMin(ITestExecutor executor, List<ICodeFragment> initialConfiguration, int totalFragments) {
		Map<String, ITestExecutor.ETestResult> resultMap = new HashMap<>();

		checkPreconditions(executor, initialConfiguration, totalFragments, resultMap);

		m_fragments = new ArrayList<>(initialConfiguration);
		int granularity = 2;
		while (m_fragments.size() >= 2) {
			List<List<ICodeFragment>> subsets = CollectionsUtility.split(m_fragments, granularity);
			assert subsets.size() == granularity;

			boolean someComplementIsFailing = false;
			List<Callable<List<ICodeFragment>>> taskList = new ArrayList<>();
			for (List<ICodeFragment> subset : subsets) {
				if (m_isCancelled.get()) {
					throw new CancellationException("Cancelled by user");
				}
				List<ICodeFragment> complement = CollectionsUtility.listMinus(m_fragments, subset);

				if (m_testExecutorOptions.getNumberOfThreads() > 1) {
					taskList.add(() -> {
						if (!m_isCancelled.get() && executeTest(executor, complement, totalFragments, resultMap) == ITestExecutor.ETestResult.FAILED) {
							return complement;
						}
						throw new Exception("Test did not fail");
					});
				} else {
					if (executeTest(executor, complement, totalFragments, resultMap) == ITestExecutor.ETestResult.FAILED) {
						m_fragments = complement;
						someComplementIsFailing = true;
						break;
					}
				}
			}
			if (m_testExecutorOptions.getNumberOfThreads() > 1) {
				try {
					m_fragments = m_executorService.invokeAny(taskList);
					someComplementIsFailing = true;
				} catch (ExecutionException e) {
					// no task completed successfully -> increase granularity
				} catch (InterruptedException e) {
					throw new TestingException("Exception occured when running ddmin concurrently.", e);
				}
			}

			if (someComplementIsFailing) {
				granularity = Math.max(granularity - 1, 2);
				logDebug("DDmin: granularity decreased to " + granularity + " / " + m_fragments.size());
			} else {
				if (granularity == m_fragments.size()) {
					break;
				}

				granularity = Math.min(granularity * 2, m_fragments.size());
				logDebug("DDmin: granularity increased to " + granularity + " / " + m_fragments.size());
			}
		}
		return m_fragments;
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
		log(":::: " + result + " :::: size: " + configuration.size() + " / " + totalFragments + " :::: " + executor.getStatistics(), result == ITestExecutor.ETestResult.FAILED ? TestExecutorOptions.ELogLevel.INFO : TestExecutorOptions.ELogLevel.DEBUG);
		resultMap.put(configurationIdentifier, result);

		return result;
	}

	protected String getConfigurationIdentifier(List<ICodeFragment> configuration, int totalFragments) {
		List<Long> activeFragments = LongStream.range(0, totalFragments)
				.mapToObj(i -> 0L)
				.collect(Collectors.toList());

		configuration.forEach(fr -> activeFragments.set((int) fr.getFragmentNumber(), 1L));

		return activeFragments.stream().map(i -> Long.toString(i)).collect(Collectors.joining());
	}

	protected abstract ITestExecutor getTestExecutor();

	protected void printConfigurationInfo(List<ICodeFragment> minConfig, List<ICodeFragment> fragments) {
		StringBuilder sb = new StringBuilder();
		for (ICodeFragment fr : fragments) {
			if (minConfig.contains(fr)) {
				sb.append(1);
			} else {
				sb.append(0);
			}
		}
		logInfo(sb);
	}

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
