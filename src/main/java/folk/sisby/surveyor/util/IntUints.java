package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;

public record IntUints(int value) implements UIntArray {
    @Override
    public int[] getUncompressed() {
        return ArrayUtil.ofSingle(value, 256);
    }

    @Override
    public int[] getUnmasked(UIntArray mask) {
        return ArrayUtil.ofSingle(value, 256);
    }

    @Override
    public void writeNbt(NbtCompound nbt, String key) {
        if (value != -1) nbt.putInt(key, value);
    }

    @Override
    public boolean isEmpty(int i) {
        return value == -1;
    }

    @Override
    public int get(int i) {
        return value;
    }
}
