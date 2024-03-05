package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.structure.StructurePieceSummary;
import folk.sisby.surveyor.structure.StructureSummary;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public record S2CStructuresAddedPacket(Map<ChunkPos, Map<RegistryKey<Structure>, Pair<RegistryKey<StructureType<?>>, Collection<StructurePieceSummary>>>> structures) implements S2CPacket {
    public static S2CStructuresAddedPacket of(StructureSummary summary) {
        return new S2CStructuresAddedPacket(Map.of(summary.getPos(), Map.of(summary.getKey(), Pair.of(summary.getType(), summary.getChildren()))));
    }

    public static S2CStructuresAddedPacket read(PacketByteBuf buf) {
        return new S2CStructuresAddedPacket(
            buf.readMap(
                b -> new ChunkPos(b.readVarInt(), b.readVarInt()),
                b -> b.readMap(
                    b2 -> b2.readRegistryKey(RegistryKeys.STRUCTURE),
                    b2 -> Pair.of(
                        b2.readRegistryKey(RegistryKeys.STRUCTURE_TYPE),
                        b2.readList(b3 -> WorldStructureSummary.readStructurePieceNbt(Objects.requireNonNull(b2.readNbt())))
                    )
                )
            )
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(structures, PacketByteBuf::writeChunkPos,
            (b, posMap) -> b.writeMap(posMap, PacketByteBuf::writeRegistryKey, (b2, pair) -> {
                b2.writeRegistryKey(pair.left());
                b2.writeCollection(pair.right(), (b3, piece) -> b3.writeNbt(piece.writeNbt(new NbtCompound())));
            }));
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.S2C_STRUCTURES_ADDED;
    }
}
