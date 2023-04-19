package testexecutor.gdd;

import fragment.ICodeFragment;
import testexecutor.TestExecutorOptions;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

public class GDDrecTestExecutor extends GDDTestExecutor {

    protected Queue<Long> queue = new ArrayDeque<>();

    public GDDrecTestExecutor(TestExecutorOptions options) {
        super(options);
    }

    @Override
    public List<ICodeFragment> getActiveFragments() {
        if (!queue.isEmpty()) {
            long currentNodeId = queue.poll();
            m_activeFragments = m_graphDB.calculateActiveFragmentsDependentOn(currentNodeId);
            if (!m_activeFragments.isEmpty()) {
                return m_activeFragments.stream()
                        .map(m_fragments::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        }

        return super.getActiveFragments();
    }

    @Override
    public void addFixedFragments(List<ICodeFragment> fragments) {
        super.addFixedFragments(fragments);
        queue.addAll(fragments.stream()
                .map(ICodeFragment::getFragmentNumber)
                .collect(Collectors.toSet()));
    }
}
