package folk.sisby.surveyor.util;

import com.google.common.primitives.Bytes;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;

import java.util.List;
import java.util.Objects;

public interface UIntArray {
    int UINT_BYTE_OFFSET = 127;

    int[] getUncompressed();

    void writeNbt(NbtCompound nbt, String key);

    boolean isEmpty(int i);

    int get(int i);

    static UIntArray readNbt(NbtElement nbt) {
        if (nbt == null) return null;
        return switch (nbt.getType()) {
            case NbtElement.BYTE_TYPE -> new ByteUInts(((NbtByte) nbt).byteValue());
            case NbtElement.BYTE_ARRAY_TYPE -> new ByteArrayUInts(((NbtByteArray) nbt).getByteArray());
            case NbtElement.INT_TYPE -> new IntUints(((NbtInt) nbt).intValue());
            case NbtElement.INT_ARRAY_TYPE -> new IntArrayUInts(((NbtIntArray) nbt).getIntArray());
            default -> throw new IllegalStateException("Unexpected value: " + nbt.getType());
        };
    }

    static UIntArray fromUInts(List<Integer> ints) {
        long distinct = ints.stream().filter(Objects::nonNull).distinct().count();
        Integer single = distinct == 1 ? ints.stream().filter(Objects::nonNull).distinct().findFirst().get() : null;
        if (distinct == 0 || Integer.valueOf(-1).equals(single)) return null;
        boolean oneByte = ints.stream().filter(Objects::nonNull).allMatch(i -> i >= -128 + UINT_BYTE_OFFSET && i < 127 + UINT_BYTE_OFFSET);
        if (single != null)
            if (oneByte) {
                return new ByteUInts((byte) (single - UINT_BYTE_OFFSET));
            } else {
                return new IntUints(single);
            }
        else {
            if (oneByte) {
                return new ByteArrayUInts(Bytes.toArray(ints.stream().mapToInt(i -> Objects.requireNonNullElse(i, -1)).mapToObj(i -> (byte) (i - UINT_BYTE_OFFSET)).toList()));
            } else {
                return new IntArrayUInts(ints.stream().mapToInt(i -> Objects.requireNonNullElse(i, -1)).toArray());
            }
        }
    }
}
