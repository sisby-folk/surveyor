package folk.sisby.surveyor.structure;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class StructurePieceSummary extends StructurePiece {
    public StructurePieceSummary(StructurePieceType type, int chainLength, BlockBox boundingBox) {
        super(type, chainLength, boundingBox);
    }

    public StructurePieceSummary(NbtCompound nbt) {
        super(Registries.STRUCTURE_PIECE.get(new Identifier(nbt.getString("id"))), nbt);
    }

    public static StructurePieceSummary fromPiece(StructurePiece piece) {
        return new StructurePieceSummary(piece.getType(), piece.getChainLength(), piece.getBoundingBox());
    }

    public final NbtCompound toNbt() {
        return toNbt(null); // context only used for writeNbt
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
    }
}
