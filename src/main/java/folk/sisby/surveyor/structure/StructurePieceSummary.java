package folk.sisby.surveyor.structure;

import folk.sisby.surveyor.Surveyor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;

public class StructurePieceSummary {
    public static final String KEY_TYPE = "type";
    public static final String KEY_BOX = "BB";
    public final StructurePieceType type;
    public final BlockBox boundingBox;

    public StructurePieceSummary(StructurePieceType type, BlockBox boundingBox) {
        this.type = type;
        this.boundingBox = boundingBox;
    }

    public static StructurePieceSummary fromPiece(StructurePiece piece) {
        return new StructurePieceSummary(piece.getType(), piece.getBoundingBox());
    }

    public static StructurePieceSummary fromNbt(NbtCompound nbt) {
        return new StructurePieceSummary(
            Registries.STRUCTURE_PIECE.get(new Identifier(nbt.getString(KEY_TYPE))),
            BlockBox.CODEC
                .parse(NbtOps.INSTANCE, nbt.get("BB"))
                .resultOrPartial(Surveyor.LOGGER::error)
                .orElseThrow(() -> new IllegalArgumentException("Invalid boundingbox"))
        );
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putString(KEY_TYPE, Registries.STRUCTURE_PIECE.getId(type).toString());
        BlockBox.CODEC.encodeStart(NbtOps.INSTANCE, this.boundingBox).resultOrPartial(Surveyor.LOGGER::error).ifPresent(element -> nbt.put(KEY_BOX, element));
        return nbt;
    }
}
