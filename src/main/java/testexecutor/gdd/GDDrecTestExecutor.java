package testexecutor.gdd;

import fragment.ICodeFragment;
import testexecutor.TestExecutorOptions;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

public class GDDrecTestExecutor extends GDDTestExecutor {

    protected Queue<Long> m_queue = new ArrayDeque<>();
    protected Long m_activeParentNode = null;

    public GDDrecTestExecutor(TestExecutorOptions options) {
        super(options);
    }

    @Override
    public List<ICodeFragment> getActiveFragments() {
        while (!m_queue.isEmpty()) {
            m_activeParentNode = m_queue.poll();
            m_activeFragments = m_graphDB.calculateActiveFragmentsDependentOn(m_activeParentNode);
            if (!m_activeFragments.isEmpty()) {
                return m_activeFragments.stream()
                        .map(m_fragments::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            if (m_graphDB.checkForFreeDependentNodes(m_activeParentNode) > 0) {
                m_queue.add(m_activeParentNode);
            }
        }

        return super.getActiveFragments();
    }

    @Override
    public void addFixedFragments(List<ICodeFragment> fragments) {
        super.addFixedFragments(fragments);
        m_queue.addAll(fragments.stream()
                .map(ICodeFragment::getFragmentNumber)
                .collect(Collectors.toSet()));
    }
}
