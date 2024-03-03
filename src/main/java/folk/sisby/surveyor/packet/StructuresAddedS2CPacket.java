package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.structure.StructureSummary;
import folk.sisby.surveyor.structure.StructurePieceSummary;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public record StructuresAddedS2CPacket(Map<ChunkPos, Map<RegistryKey<Structure>, Pair<RegistryKey<StructureType<?>>, Collection<StructurePieceSummary>>>> structures) implements S2CPacket {
    public static StructuresAddedS2CPacket of(StructureSummary summary) {
        return new StructuresAddedS2CPacket(Map.of(summary.getPos(), Map.of(summary.getKey(), Pair.of(summary.getType(), summary.getChildren()))));
    }

    public static StructuresAddedS2CPacket read(PacketByteBuf buf) {
        return new StructuresAddedS2CPacket(
            buf.readMap(
                b -> new ChunkPos(b.readVarInt(), b.readVarInt()),
                b -> b.readMap(
                    b2 -> RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(b.readString())),
                    b2 -> Pair.of(
                        RegistryKey.of(RegistryKeys.STRUCTURE_TYPE, new Identifier(b.readString())),
                        b2.readCollection(ArrayList::new, b3 -> WorldStructureSummary.readStructurePieceNbt(Objects.requireNonNull(b2.readNbt())))
                    )
                )
            )
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(structures, (b, pos) -> {
            b.writeVarInt(pos.x);
            b.writeVarInt(pos.z);
        }, (b, posMap) -> {
            b.writeMap(posMap, (b2, key) -> {
                b2.writeString(key.getValue().toString());
            }, (b2, pair) -> {
                b2.writeString(pair.left().getValue().toString());
                b2.writeCollection(pair.right(), (b3, piece) -> {
                    b3.writeNbt(piece.writeNbt(new NbtCompound()));
                });
            });
        });
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.S2C_STRUCTURES_ADDED;
    }
}
