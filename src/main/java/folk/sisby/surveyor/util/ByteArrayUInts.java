package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;

public record ByteArrayUInts(byte[] value) implements UIntArray {
    @Override
    public int[] getUncompressed() {
        int[] uncompressed = ArrayUtil.ofSingle(-1, 256);
        for (int i = 0; i < value.length; i++) {
            uncompressed[i] = value[i] + UINT_BYTE_OFFSET;
        }
        return uncompressed;
    }

    @Override
    public int[] getUnmasked(UIntArray mask) {
        int[] unmasked = new int[256];
        int maskedIndex = 0;
        for (int i = 0; i < 256; i++) {
            if (!mask.isEmpty(i)) {
                unmasked[i] = value[maskedIndex] + UINT_BYTE_OFFSET;
                maskedIndex++;
            }
        }
        return unmasked;
    }

    @Override
    public void writeNbt(NbtCompound nbt, String key) {
        nbt.putByteArray(key, value);
    }

    @Override
    public boolean isEmpty(int i) {
        return i > value.length - 1 || value[i] == -128;
    }

    @Override
    public int get(int i) {
        return value[i] + UINT_BYTE_OFFSET;
    }
}
