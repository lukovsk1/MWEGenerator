package testexecutor.hdd;

import fragment.HDDCodeFragment;
import fragment.ICodeFragment;
import fragment.IHierarchicalCodeFragment;
import testexecutor.TestExecutorOptions;
import utility.CollectionsUtility;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/*
    An executor that extracts the hierarchical code fragments from the AST of the source code
 */
public class HDDrecTestExecutor extends HDDTestExecutor {

	protected Queue<IHierarchicalCodeFragment> queue = new ArrayDeque<>();

	public HDDrecTestExecutor(TestExecutorOptions options) {
		super(options);
	}

	public Queue<IHierarchicalCodeFragment> getQueue() {
		return queue;
	}

	@Override
	public List<ICodeFragment> extractFragments() {
		List<ICodeFragment> fileRoots = super.extractFragments();
		addFileRootsToQueue(fileRoots);
		return fileRoots;
	}

	public void addFileRootsToQueue(List<ICodeFragment> fileRoots) {
		HDDCodeFragment root = new HDDCodeFragment("", -1);
		fileRoots.stream().map(HDDCodeFragment.class::cast).forEach(root::addChild);
		queue.add(root);
	}

	@Override
	protected Map<String, String> mapFragmentsToFiles(List<ICodeFragment> fragments) {
		return super.mapFragmentsToFiles(CollectionsUtility.union(fragments, queue));
	}
}
