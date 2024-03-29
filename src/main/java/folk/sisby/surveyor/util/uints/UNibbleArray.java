package folk.sisby.surveyor.util.uints;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;

public record UNibbleArray(byte[] value) implements ArrayUInts {
    public static final byte TYPE = NbtElement.DOUBLE_TYPE;

    public static UInts ofInts(int[] value) {
        byte[] packed = new byte[value.length / 2 + (value.length & 1)];
        for (int i = 0; i < value.length; i+= 2) {
            packed[i / 2] |= (byte) (value[i] << NIBBLE_SIZE);
        }
        for (int i = 1; i < value.length; i+= 2) {
            packed[i / 2] |= (byte) value[i];
        }
        return new UNibbleArray(packed);
    }

    public static UInts fromBuf(PacketByteBuf buf) {
        return new UNibbleArray(buf.readByteArray());
    }

    @Override
    public int get(int i) {
        return (i & 1) == 0 ? value[i / 2] >>> NIBBLE_SIZE : value[i / 2] & NIBBLE_MASK;
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
