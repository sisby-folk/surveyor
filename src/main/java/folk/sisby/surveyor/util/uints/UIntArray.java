package folk.sisby.surveyor.util.uints;

import com.google.common.primitives.Bytes;
import folk.sisby.surveyor.util.ArrayUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Function;

/**
 * A compressed representation of an int array for holding unsigned ints.
 * Keeps size down in memory, as well as in NBT and packets.
 * Designed to be paired with a bitset as a mask, so it can represent nullable values within a static-sized array.
 *
 * @author Sisby folk
 * @author Falkreon
 * @author Ampflower
 */
public interface UIntArray {
    int UINT_BYTE_OFFSET = 128;
    int NULL_TYPE = NbtElement.END_TYPE;

    static boolean fitsInByte(int value) {
        return value >= 0 && value <= 255;
    }

    int getType();

    int[] getUnmasked(BitSet mask);

    void writeNbt(NbtCompound nbt, String key);

    void writeBuf(PacketByteBuf buf);

    int get(int i);

    UIntArray remap(Function<Integer, Integer> remapping, int defaultValue, int cardinality);

    static UIntArray remap(UIntArray input, Function<Integer, Integer> remapping, int defaultValue, int cardinality) {
        return (input == null ? new IntUints(defaultValue) : input).remap(remapping, defaultValue, cardinality);
    }

    default int getMasked(BitSet mask, int i) {
        int empty = 0;
        for (int j = 0; j < i; j++) {
            if (!mask.get(j)) empty++;
        }
        return get(i - empty);
    }

    static void writeBuf(UIntArray array, PacketByteBuf buf) {
        if (array == null) {
            buf.writeVarInt(NULL_TYPE);
            return;
        }
        buf.writeVarInt(array.getType());
        array.writeBuf(buf);
    }

    static UIntArray readNbt(NbtElement nbt) {
        if (nbt == null) return null;
        return switch (nbt.getType()) {
            case ByteUInts.TYPE -> ByteUInts.fromNbt(nbt);
            case ByteArrayUInts.TYPE -> ByteArrayUInts.fromNbt(nbt);
            case IntUints.TYPE -> IntUints.fromNbt(nbt);
            case IntArrayUInts.TYPE -> IntArrayUInts.fromNbt(nbt);
            default -> throw new IllegalStateException("UIntArray encountered unexpected NBT type: " + nbt.getType());
        };
    }

    static UIntArray readBuf(PacketByteBuf buf) {
        int type = buf.readVarInt();
        return switch (type) {
            case UIntArray.NULL_TYPE -> null;
            case ByteUInts.TYPE -> ByteUInts.fromBuf(buf);
            case ByteArrayUInts.TYPE -> ByteArrayUInts.fromBuf(buf);
            case IntUints.TYPE -> IntUints.fromBuf(buf);
            case IntArrayUInts.TYPE -> IntArrayUInts.fromBuf(buf);
            default -> throw new IllegalStateException("UIntArray encountered unexpected buf type: " + type);
        };
    }

    static UIntArray fromUInts(int[] ints, int defaultValue) {
        if (ints.length == 0) return null;
        long distinct = ArrayUtil.distinctCount(ints);
        if (distinct == 1 && ints[0] == defaultValue) return null;
        boolean oneByte = Arrays.stream(ints).allMatch(UIntArray::fitsInByte);
        if (distinct == 1)
            if (oneByte) {
                return new ByteUInts((byte) (ints[0] - UINT_BYTE_OFFSET));
            } else {
                return new IntUints(ints[0]);
            }
        else {
            int lastIndex = ArrayUtil.trimIndex(ints, defaultValue);
            if (oneByte) {
                return new ByteArrayUInts(Bytes.toArray(Arrays.stream(ints).mapToObj(i -> (byte) (i - UINT_BYTE_OFFSET)).toList().subList(0, lastIndex)));
            } else {
                return new IntArrayUInts(Arrays.copyOfRange(ints, 0, lastIndex));
            }
        }
    }
}
