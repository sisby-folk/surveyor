package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.network.PacketByteBuf;

import java.util.BitSet;

public record IntArrayUInts(int[] value) implements UIntArray {
    public static final int TYPE = NbtElement.INT_ARRAY_TYPE;

    public static UIntArray fromNbt(NbtElement nbt) {
        return new IntArrayUInts(((NbtIntArray) nbt).getIntArray());
    }

    public static UIntArray fromBuf(PacketByteBuf buf) {
        return new IntArrayUInts(buf.readIntArray());
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public int[] getUncompressed() {
        int[] uncompressed = ArrayUtil.ofSingle(-1, 256);
        System.arraycopy(value, 0, uncompressed, 0, value.length);
        return uncompressed;
    }

    @Override
    public int[] getUnmasked(BitSet mask) {
        int[] unmasked = new int[256];
        int maskedIndex = 0;
        for (int i = 0; i < 256; i++) {
            if (mask.get(i)) {
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
    public void writeBuf(PacketByteBuf buf) {
        buf.writeIntArray(value);
    }

    @Override
    public int get(int i) {
        return value[i];
    }
}
