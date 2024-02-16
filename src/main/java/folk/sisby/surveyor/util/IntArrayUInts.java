package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;

public record IntArrayUInts(int[] value) implements UIntArray {
    @Override
    public int[] getUncompressed() {
        return value;
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
