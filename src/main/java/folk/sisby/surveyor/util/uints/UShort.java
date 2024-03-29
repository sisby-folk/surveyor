package folk.sisby.surveyor.util.uints;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtShort;
import net.minecraft.network.PacketByteBuf;

public record UShort(short value) implements SingleUInts {
    public static final byte TYPE = NbtElement.SHORT_TYPE;

    public static UInts ofInt(int value) {
        return new UShort((short) value);
    }

    public static UInts fromNbt(NbtElement nbt) {
        return new UShort(((NbtShort) nbt).shortValue());
    }

    public static UInts fromBuf(PacketByteBuf buf) {
        return new UShort(buf.readShort());
    }

    @Override
    public int get() {
        return value & SHORT_MASK;
    }

    @Override
    public void writeNbt(NbtCompound nbt, String key) {
        nbt.putShort(key, value);
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeShort(value);
    }

    @Override
    public int getType() {
        return TYPE;
    }
}
