package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;

import java.util.Collections;

public record ByteUInts(byte value) implements UIntArray {
    @Override
    public int[] getUncompressed() {
        return Collections.nCopies(255, value + UINT_BYTE_OFFSET).stream().mapToInt(i -> i).toArray();
    }

    @Override
    public void writeNbt(NbtCompound nbt, String key) {
        if (value != -128) nbt.putByte(key, value);
    }

    @Override
    public boolean isEmpty(int i) {
        return value == -128;
    }

    @Override
    public int get(int i) {
        return value + UINT_BYTE_OFFSET;
    }
}
