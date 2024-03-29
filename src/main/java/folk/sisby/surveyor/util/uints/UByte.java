package folk.sisby.surveyor.util.uints;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;

public record UByte(byte value) implements SingleUInts {
    public static final byte TYPE = NbtElement.BYTE_TYPE;

    public static UInts ofInt(int value) {
        return new UByte((byte) value);
    }

    public static UInts fromNbt(NbtElement nbt) {
        return new UByte(((NbtByte) nbt).byteValue());
    }

    public static UInts fromBuf(PacketByteBuf buf) {
        return new UByte(buf.readByte());
    }

    @Override
    public int get() {
        return value & BYTE_MASK;
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
    public int getType() {
        return TYPE;
    }
}
