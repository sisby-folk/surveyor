package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.IntStream;

public class NbtUtil {
    public static int UINT_OFFSET = 127;

    public static int[] readOptionalUInts(NbtElement nbt) {
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

    private static void writeUInts(NbtCompound nbt, String key, int[] array, boolean nullOptional) {
        long distinct = Arrays.stream(array).filter(i -> !nullOptional || i >= 0).distinct().count();
        Integer single = distinct == 1 ? Arrays.stream(array).filter(i -> !nullOptional || i >= 0).distinct().findFirst().getAsInt() : null;
        if (Integer.valueOf(-1).equals(single)) return;
        boolean oneByte = Arrays.stream(array).filter(Objects::nonNull).allMatch(i -> i >= -128 + UINT_OFFSET && i < 127 + UINT_OFFSET);
        if (single != null && oneByte) nbt.putByte(key, (byte) (single - UINT_OFFSET));
        if (single == null && oneByte) nbt.putByteArray(key, Arrays.stream(array).mapToObj((i -> (byte) (i - UINT_OFFSET))).toList());
        if (single != null && !oneByte) nbt.putInt(key, single);
        if (single == null && !oneByte) nbt.putIntArray(key, array);
    }

    public static void writeOptionalUInts(NbtCompound nbt, String key, int[] array) {
        writeUInts(nbt, key, array, true);
    }

    public static void writeUInts(NbtCompound nbt, String key, int[] array) {
        writeUInts(nbt, key, array, false);
    }
}
