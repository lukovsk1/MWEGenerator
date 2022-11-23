package slice;

import java.util.List;

public interface IHierarchicalCodeSlice {

	int getLevel();

	List<IHierarchicalCodeSlice> getChildren();
}
