package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class NbtUtil {
    public static final int UINT_OFFSET = 127;

    public static int[] readUInts(NbtElement nbt) {
        if (nbt == null) return Collections.nCopies(255, -1).stream().mapToInt(i -> i).toArray();
        return switch (nbt.getType()) {
            case NbtElement.BYTE_TYPE -> Collections.nCopies(255, ((NbtByte) nbt).intValue() + UINT_OFFSET).stream().mapToInt(i -> i).toArray();
            case NbtElement.BYTE_ARRAY_TYPE -> {
                byte[] bytes = ((NbtByteArray) nbt).getByteArray();
                yield IntStream.range(0, bytes.length).map(i -> bytes[i] + UINT_OFFSET).toArray();
            }
            case NbtElement.INT_TYPE -> Collections.nCopies(255, ((NbtInt) nbt).intValue()).stream().mapToInt(i -> i).toArray();
            case NbtElement.INT_ARRAY_TYPE -> ((NbtIntArray) nbt).getIntArray();
            default -> throw new IllegalStateException("Unexpected value: " + nbt.getType());
        };
    }

    public static void writeNullableUInts(NbtCompound nbt, String key, List<Integer> ints) {
        long distinct = ints.stream().filter(Objects::nonNull).distinct().count();
        Integer single = distinct == 1 ? ints.stream().filter(Objects::nonNull).distinct().findFirst().get() : null;
        if (distinct == 0 || Integer.valueOf(-1).equals(single)) return;
        boolean oneByte = ints.stream().filter(Objects::nonNull).allMatch(i -> i >= -128 + UINT_OFFSET && i < 127 + UINT_OFFSET);
        if (single != null && oneByte) nbt.putByte(key, (byte) (single - UINT_OFFSET));
        if (single != null && !oneByte) nbt.putInt(key, single);
        int[] intArray = ints.stream().mapToInt(i -> Objects.requireNonNullElse(i, -1)).toArray();
        if (single == null && oneByte) nbt.putByteArray(key, Arrays.stream(intArray).mapToObj((i -> (byte) (i - UINT_OFFSET))).toList());
        if (single == null && !oneByte) nbt.putIntArray(key, intArray);
    }
}
