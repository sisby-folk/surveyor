package folk.sisby.surveyor.util.uints;

import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;

public record UByteArray(byte[] value) implements ArrayUInts {
    public static final byte TYPE = NbtElement.BYTE_ARRAY_TYPE;

    public static UInts ofInts(int[] ints) {
        byte[] value = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            value[i] = (byte) ints[i];
        }
        return new UByteArray(value);
    }

    public static UInts fromNbt(NbtElement nbt, int cardinality) {
        byte[] value = ((NbtByteArray) nbt).getByteArray();
        return value.length == cardinality ? new UByteArray(value) : new UNibbleArray(value);
    }

    public static UInts fromBuf(PacketByteBuf buf) {
        return new UByteArray(buf.readByteArray());
    }

    @Override
    public int get(int i) {
        return value[i] & BYTE_MASK;
    }

    @Override
    public void writeNbt(NbtCompound nbt, String key) {
        nbt.putByteArray(key, value);
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeByteArray(value);
    }

    @Override
    public int getType() {
        return TYPE;
    }
}
