package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;

import java.util.Collections;
import java.util.Map;

public record IntUints(int value) implements UIntArray {
    @Override
    public int[] getUncompressed() {
        return Collections.nCopies(256, value).stream().mapToInt(i -> i).toArray();
    }

    @Override
    public UIntArray map(Map<Integer, Integer> mapping, Integer defaultValue) {
        int newValue = mapping.get(get(0));
        return UIntArray.fitsInByte(newValue) ? new ByteUInts((byte) (value - UINT_BYTE_OFFSET)) : new IntUints(value);
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
