package folk.sisby.surveyor.util.uints;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.network.PacketByteBuf;

public record UInt(int value) implements SingleUInts {
	public static final byte TYPE = NbtElement.INT_TYPE;

	public static UInts ofInt(int value) {
		return new UInt(value);
	}

	public static UInts fromNbt(NbtElement nbt) {
		return new UInt(((NbtInt) nbt).intValue());
	}

	public static UInts fromBuf(PacketByteBuf buf) {
		return new UInt(buf.readVarInt());
	}

	@Override
	public int get() {
		return value;
	}

	@Override
	public void writeNbt(NbtCompound nbt, String key) {
		nbt.putInt(key, value);
	}

	@Override
	public void writeBuf(PacketByteBuf buf) {
		buf.writeVarInt(value);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
