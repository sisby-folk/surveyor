package folk.sisby.surveyor.util;

import com.google.common.primitives.Bytes;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;

import java.util.Arrays;

public interface UIntArray {
    int UINT_BYTE_OFFSET = 127;

    static boolean fitsInByte(int value) {
        return value >= -128 + UINT_BYTE_OFFSET && value < 127 + UINT_BYTE_OFFSET;
    }

    int[] getUncompressed();

    int[] getUnmasked(UIntArray mask);

    void writeNbt(NbtCompound nbt, String key);

    boolean isEmpty(int i);

    int get(int i);

    default int getMasked(UIntArray mask, int i) {
        int empty = 0;
        for (int j = 0; j < i; j++) {
            if (mask.isEmpty(j)) empty++;
        }
        return get(i - empty);
    }

    static UIntArray readNbt(NbtElement nbt, int defaultValue) {
        if (nbt == null) return null;
        return fromUInts((switch (nbt.getType()) { // Recompress on read.
            case NbtElement.BYTE_TYPE -> new ByteUInts(((NbtByte) nbt).byteValue());
            case NbtElement.BYTE_ARRAY_TYPE -> new ByteArrayUInts(((NbtByteArray) nbt).getByteArray());
            case NbtElement.INT_TYPE -> new IntUints(((NbtInt) nbt).intValue());
            case NbtElement.INT_ARRAY_TYPE -> new IntArrayUInts(((NbtIntArray) nbt).getIntArray());
            default -> throw new IllegalStateException("Unexpected value: " + nbt.getType());
        }).getUncompressed(), defaultValue);
    }

    static UIntArray fromUInts(int[] ints, int defaultValue) {
        if (ints.length == 0) return null;
        long distinct = ArrayUtil.distinctCount(ints);
        if (distinct == 1 && ints[0] == defaultValue) return null;
        boolean oneByte = Arrays.stream(ints).allMatch(UIntArray::fitsInByte);
        if (distinct == 1)
            if (oneByte) {
                return new ByteUInts((byte) (ints[0] - UINT_BYTE_OFFSET));
            } else {
                return new IntUints(ints[0]);
            }
        else {
            int lastIndex = ArrayUtil.trimIndex(ints, -1);
            if (oneByte) {
                return new ByteArrayUInts(Bytes.toArray(Arrays.stream(ints).mapToObj(i -> (byte) (i - UINT_BYTE_OFFSET)).toList().subList(0, lastIndex)));
            } else {
                return new IntArrayUInts(Arrays.copyOfRange(ints,0, lastIndex));
            }
        }
    }
}
