package folk.sisby.surveyor.util.uints;

import folk.sisby.surveyor.util.ArrayUtil;

import java.util.BitSet;
import java.util.function.Function;

public interface ArrayUInts extends UInts {
    @Override
    default int[] getUnmasked(BitSet mask) {
        int[] unmasked = new int[mask.size()];
        int maskedIndex = 0;
        for (int i = 0; i < unmasked.length; i++) {
            if (mask.get(i)) {
                unmasked[i] = get(maskedIndex);
                maskedIndex++;
            }
        }
        return unmasked;
    }

    @Override
    default UInts remap(Function<Integer, Integer> remapping, int defaultValue, int cardinality) {
        int[] newArray = ArrayUtil.ofSingle(defaultValue, cardinality);
        for (int i = 0; i < newArray.length; i++) {
            int val = get(i);
            Integer mapped = remapping.apply(val);
            newArray[i] = mapped == null ? val : mapped;
        }
        return UInts.fromUInts(newArray, defaultValue);
    }
}
