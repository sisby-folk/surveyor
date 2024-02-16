package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;

import java.util.Collections;

public record IntUints(int value) implements UIntArray {
    @Override
    public int[] getUncompressed() {
        return Collections.nCopies(255, value).stream().mapToInt(i -> i).toArray();
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
