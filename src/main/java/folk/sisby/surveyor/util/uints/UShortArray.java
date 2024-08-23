package folk.sisby.surveyor.util.uints;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;

public record UShortArray(short[] value) implements ArrayUInts {
	public static final int TYPE = NbtElement.STRING_TYPE;

	public static UInts ofInts(int[] ints) {
		short[] value = new short[ints.length];
		for (int i = 0; i < ints.length; i++) {
			value[i] = (short) ints[i];
		}
		return new UShortArray(value);
	}

	public static UInts ofPacked(int[] ints, int cardinality) {
		short[] value = new short[cardinality];
		for (int i = 0; i < value.length; i += 2) {
			value[i] = (short) (ints[i / 2] >>> Short.SIZE);
		}
		for (int i = 1; i < value.length; i += 2) {
			value[i] = (short) (ints[i / 2] & SHORT_MASK);
		}
		return new UShortArray(value);
	}

	public static UInts fromBuf(PacketByteBuf buf, int cardinality) {
		return ofPacked(buf.readIntArray(), cardinality);
	}

	public int[] packToInts() {
		int[] packed = new int[value.length / 2 + (value.length & 1)];
		for (int i = 0; i < value.length; i += 2) {
			packed[i / 2] |= value[i] << Short.SIZE;
		}
		for (int i = 1; i < value.length; i += 2) {
			packed[i / 2] |= value[i];
		}
		return packed;
	}

	@Override
	public int get(int i) {
		return value[i] & SHORT_MASK;
	}

	@Override
	public void writeNbt(NbtCompound nbt, String key) {
		nbt.putIntArray(key, packToInts());
	}

	@Override
	public void writeBuf(PacketByteBuf buf) {
		buf.writeIntArray(packToInts());
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
