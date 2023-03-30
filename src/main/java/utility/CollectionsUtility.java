package utility;

import fragment.IHierarchicalCodeFragment;

import java.util.*;
import java.util.stream.Collectors;

public final class CollectionsUtility {

	private CollectionsUtility() {
	}

	/*
		Split a configuration of fragments into N subsets
		return the list of subsets
	 */
	public static <T> List<List<T>> split(List<T> fragments, int granularity) {
		List<List<T>> subsets = new ArrayList<>();
		int start = 0;
		for (int i = 0; i < granularity; i++) {
			int subsetLength = (int) ((fragments.size() - start) / (float) (granularity - i) + 0.5f);
			List<T> subset = fragments.subList(start, start + subsetLength);
			subsets.add(subset);
			start += subsetLength;
		}

		assert subsets.size() == granularity;
		for (List<T> subset : subsets) {
			assert subset.size() > 0;
		}
		return subsets;
	}

	/*
		Return all elements of c1 that are not in c2.
	 */
	public static <T> List<T> listMinus(List<T> c1, List<T> c2) {
		return c1.stream()
				.filter(e -> !c2.contains(e))
				.collect(Collectors.toList());
	}

	// returns a set of IHierarchicalCodeFragments including the fragment itself and all of its descendents
	public static Set<IHierarchicalCodeFragment> getChildrenInDeep(IHierarchicalCodeFragment fr) {
		if (fr == null) {
			return Collections.emptySet();
		}
		if (fr.getChildren().isEmpty()) {
			return Collections.singleton(fr);
		}
		Set<IHierarchicalCodeFragment> returnValue = new HashSet<>();
		returnValue.add(fr);
		for (IHierarchicalCodeFragment child : fr.getChildren()) {
			returnValue.addAll(getChildrenInDeep(child));
		}
		return returnValue;
	}
}
