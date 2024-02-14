package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class NbtUtil {
    public static int UINT_OFFSET = 127;

    public static int[] readCompressedInts(NbtElement element) {
        return switch (element.getType()) {
            case NbtElement.BYTE_TYPE -> Collections.nCopies(255, ((NbtByte) element).intValue() + UINT_OFFSET).stream().mapToInt(i -> i).toArray();
            case NbtElement.BYTE_ARRAY_TYPE -> {
                byte[] bytes = ((NbtByteArray) element).getByteArray();
                yield IntStream.range(0, bytes.length).map(i -> bytes[i] + UINT_OFFSET).toArray();
            }
            case NbtElement.INT_TYPE -> Collections.nCopies(255, ((NbtInt) element).intValue()).stream().mapToInt(i -> i).toArray();
            case NbtElement.INT_ARRAY_TYPE -> ((NbtIntArray) element).getIntArray();
            default -> throw new IllegalStateException("Unexpected value: " + element.getType());
        };
    }

    public static void writeCompressedInts(NbtCompound compound, String key, List<Integer> list) {
        Integer single = list.stream().filter(Objects::nonNull).distinct().count() == 1 ? list.stream().filter(Objects::nonNull).distinct().findFirst().get() : null;
        boolean oneByte = list.stream().filter(Objects::nonNull).allMatch(i -> i >= -128 + UINT_OFFSET && i < 127 + UINT_OFFSET);
        if (single != null && oneByte) compound.putByte(key, (byte) (single - UINT_OFFSET));
        if (single == null && oneByte) compound.putByteArray(key, list.stream().map(i -> Objects.requireNonNullElse(i, -1)).map((i -> (byte) (i - UINT_OFFSET))).toList());
        if (single != null && !oneByte) compound.putInt(key, single);
        if (single == null && !oneByte) compound.putIntArray(key, list.stream().map(i -> Objects.requireNonNullElse(i, -1)).toList());
    }
}
