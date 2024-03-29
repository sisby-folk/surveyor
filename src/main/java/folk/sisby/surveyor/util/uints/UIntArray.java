package folk.sisby.surveyor.util.uints;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.network.PacketByteBuf;

public record UIntArray(int[] value) implements ArrayUInts {
    public static final byte TYPE = NbtElement.INT_ARRAY_TYPE;

    public static UInts ofInts(int[] ints) {
        return new UIntArray(ints);
    }

    public static UInts fromNbt(NbtElement nbt, int cardinality) {
        int[] value = ((NbtIntArray) nbt).getIntArray();
        return value.length == cardinality ? new UIntArray(value) : UShortArray.ofPacked(value, cardinality);
    }

    public static UInts fromBuf(PacketByteBuf buf) {
        return new UIntArray(buf.readIntArray());
    }

    @Override
    public int get(int i) {
        return value[i];
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
    public int getType() {
        return TYPE;
    }
}
