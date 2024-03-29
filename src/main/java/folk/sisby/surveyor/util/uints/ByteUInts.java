package folk.sisby.surveyor.util.uints;

import folk.sisby.surveyor.util.ArrayUtil;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;

import java.util.BitSet;
import java.util.function.Function;

public record ByteUInts(byte value) implements UIntArray {
    public static final int TYPE = NbtElement.BYTE_TYPE;

    public static ByteUInts fromNbt(NbtElement nbt) {
        return new ByteUInts(((NbtByte) nbt).byteValue());
    }

    public static UIntArray fromBuf(PacketByteBuf buf) {
        return new ByteUInts(buf.readByte());
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public int[] getUnmasked(BitSet mask) {
        return ArrayUtil.ofSingle(value + UINT_BYTE_OFFSET, 256);
    }

    @Override
    public void writeNbt(NbtCompound nbt, String key) {
        nbt.putByte(key, value);
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeByte(value);
    }

    @Override
    public int get(int i) {
        return value + UINT_BYTE_OFFSET;
    }

    @Override
    public UIntArray remap(Function<Integer, Integer> remapping, int defaultValue, int cardinality) {
        int newValue = remapping.apply(value + UINT_BYTE_OFFSET);
        return UIntArray.fitsInByte(newValue) ? new ByteUInts((byte) (newValue - UINT_BYTE_OFFSET)) : new IntUints(newValue);
    }
}
