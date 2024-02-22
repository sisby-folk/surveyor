package folk.sisby.surveyor.util;

import com.google.common.primitives.Bytes;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;

import java.util.Arrays;
import java.util.stream.IntStream;

public interface UIntArray {
    int UINT_BYTE_OFFSET = 127;

    static boolean fitsInByte(int value) {
        return value >= -128 + UINT_BYTE_OFFSET && value < 127 + UINT_BYTE_OFFSET;
    }

    int[] getUncompressed();

    void writeNbt(NbtCompound nbt, String key);

    boolean isEmpty(int i);

    int get(int i);

    default int getMasked(UIntArray mask, int i) {
        return get(i - (int) IntStream.range(0, i).filter(mask::isEmpty).count());
    }

    static UIntArray readNbt(NbtElement nbt, Integer defaultValue) {
        if (nbt == null) return null;
        return fromUInts((switch (nbt.getType()) { // Recompress on read.
            case NbtElement.BYTE_TYPE -> new ByteUInts(((NbtByte) nbt).byteValue());
            case NbtElement.BYTE_ARRAY_TYPE -> new ByteArrayUInts(((NbtByteArray) nbt).getByteArray());
            case NbtElement.INT_TYPE -> new IntUints(((NbtInt) nbt).intValue());
            case NbtElement.INT_ARRAY_TYPE -> new IntArrayUInts(((NbtIntArray) nbt).getIntArray());
            default -> throw new IllegalStateException("Unexpected value: " + nbt.getType());
        }).getUncompressed(), defaultValue);
    }

    static UIntArray fromUInts(int[] ints, Integer defaultValue) {
        if (ints.length == 0) return null;
        long distinct = Arrays.stream(ints).distinct().count();
        if (distinct == 1 && Integer.valueOf(ints[0]).equals(defaultValue)) return null;
        boolean oneByte = Arrays.stream(ints).allMatch(UIntArray::fitsInByte);
        if (distinct == 1)
            if (oneByte) {
                return new ByteUInts((byte) (ints[0] - UINT_BYTE_OFFSET));
            } else {
                return new IntUints(ints[0]);
            }
        else {
            if (oneByte) {
                return new ByteArrayUInts(Bytes.toArray(Arrays.stream(ints).mapToObj(i -> (byte) (i - UINT_BYTE_OFFSET)).toList()));
            } else {
                return new IntArrayUInts(ints);
            }
        }
    }
}
