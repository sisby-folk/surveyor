package folk.sisby.surveyor.util.uints;

import com.google.common.primitives.Bytes;
import folk.sisby.surveyor.util.ArrayUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;

import java.util.Arrays;
import java.util.BitSet;

/**
 * A compressed representation of an int[256] for holding optional unsigned ints.
 * Keeps size down in memory, as well as in NBT and packets.
 * Designed mostly for paletted data, as well as data with aligned "nulls" (-1 values)
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

    int[] getUncompressed();

    int[] getUnmasked(BitSet mask);

    void writeNbt(NbtCompound nbt, String key);

    void writeBuf(PacketByteBuf buf);

    int get(int i);

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

    static UIntArray readNbt(NbtElement nbt, int defaultValue) {
        if (nbt == null) return null;
        return fromUInts((switch (nbt.getType()) { // Recompress on read.
            case ByteUInts.TYPE -> ByteUInts.fromNbt(nbt);
            case ByteArrayUInts.TYPE -> ByteArrayUInts.fromNbt(nbt);
            case IntUints.TYPE -> IntUints.fromNbt(nbt);
            case IntArrayUInts.TYPE -> IntArrayUInts.fromNbt(nbt);
            default -> throw new IllegalStateException("UIntArray encountered unexpected NBT type: " + nbt.getType());
        }).getUncompressed(), defaultValue);
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
            int lastIndex = ArrayUtil.trimIndex(ints, -1);
            if (oneByte) {
                return new ByteArrayUInts(Bytes.toArray(Arrays.stream(ints).mapToObj(i -> (byte) (i - UINT_BYTE_OFFSET)).toList().subList(0, lastIndex)));
            } else {
                return new IntArrayUInts(Arrays.copyOfRange(ints,0, lastIndex));
            }
        }
    }
}
