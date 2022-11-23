package slice;

import java.util.List;

public interface IHierarchicalCodeSlice extends ICodeSlice {

	int getLevel();

	List<? extends IHierarchicalCodeSlice> getChildren();
}
