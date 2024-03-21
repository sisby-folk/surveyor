package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;

import java.util.BitSet;

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
    public int[] getUncompressed() {
        return ArrayUtil.ofSingle(value + UINT_BYTE_OFFSET, 256);
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
}
