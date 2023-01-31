package fragment;

import java.util.List;

public interface IHierarchicalCodeFragment extends ICodeFragment {

	int getLevel();

	List<? extends IHierarchicalCodeFragment> getChildren();
}
