package extractor;

import slice.ICodeSlice;

import java.util.Collections;
import java.util.List;

public class NullExtractor implements IExtractor {

    @Override
    public List<ICodeSlice> extractSlices() {
        return Collections.emptyList();
    }
}
