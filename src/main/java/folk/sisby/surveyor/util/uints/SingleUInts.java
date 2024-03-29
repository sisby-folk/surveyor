package folk.sisby.surveyor.util.uints;

import folk.sisby.surveyor.util.ArrayUtil;

import java.util.BitSet;
import java.util.function.Function;

public interface SingleUInts extends UInts {
    int get();

    @Override
    default int[] getUnmasked(BitSet mask) {
        return ArrayUtil.ofSingle(get(), mask.size());
    }

    @Override
    default int get(int i) {
        return get();
    }

    @Override
    default UInts remap(Function<Integer, Integer> remapping, int defaultValue, int cardinality) {
        return UInts.ofSingle(remapping.apply(get()), defaultValue);
    }
}
