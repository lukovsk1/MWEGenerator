package utility;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ListUtility {

    private ListUtility() {}

    /*
        Split a configuration of slices into N subsets
        return the list of subsets
     */
    public static <T> List<List<T>> split(List<T> slices, int granularity) {
        List<List<T>> subsets = new ArrayList<>();
        int start = 0;
        for(int i = 0; i < granularity; i++) {
            int subsetLength = (int) ((slices.size() - start) / (float) (granularity - i) + 0.5f);
            List<T> subset = slices.subList(start, start+subsetLength);
            subsets.add(subset);
            start += subsetLength;
        }

        assert subsets.size() == granularity;
        for(List<T> subset : subsets) {
            assert subset.size() > 0;
        }
        return subsets;
    }

    /*
        Return all elements of c1 that are not in c2.
     */
    public static <T> List<T> listMinus(List<T> c1, List<T> c2) {
        return c1.stream()
                .filter(slice -> !c2.contains(slice))
                .collect(Collectors.toList());
    }
}
