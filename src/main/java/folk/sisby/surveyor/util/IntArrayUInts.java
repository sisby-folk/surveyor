package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;

public record IntArrayUInts(int[] value) implements UIntArray {
    @Override
    public int[] getUncompressed() {
        return value;
    }

    @Override
    public int[] getUnmasked(UIntArray mask) {
        int[] unmasked = new int[256];
        int maskedIndex = 0;
        for (int i = 0; i < 256; i++) {
            if (!mask.isEmpty(i)) {
                unmasked[i] = value[maskedIndex];
                maskedIndex++;
            }
        }
        return unmasked;
    }

    @Override
    public void writeNbt(NbtCompound nbt, String key) {
        nbt.putIntArray(key, value);
    }

    @Override
    public boolean isEmpty(int i) {
        return value[i] == -1;
    }

    @Override
    public int get(int i) {
        return value[i];
    }
}
