package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;

public record ByteUInts(byte value) implements UIntArray {
    @Override
    public int[] getUncompressed() {
        return ArrayUtil.ofSingle(value + UINT_BYTE_OFFSET, 256);
    }

    @Override
    public int[] getUnmasked(UIntArray mask) {
        return ArrayUtil.ofSingle(value + UINT_BYTE_OFFSET, 256);
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
