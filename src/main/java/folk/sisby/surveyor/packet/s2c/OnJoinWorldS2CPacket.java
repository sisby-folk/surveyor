package folk.sisby.surveyor.packet.s2c;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.chunk.ChunkSummary;
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

public record OnJoinWorldS2CPacket(Map<ChunkPos, ChunkSummary> terrain, Map<ChunkPos, Map<RegistryKey<Structure>, Pair<RegistryKey<StructureType<?>>, Collection<StructurePieceSummary>>>> structures) implements S2CPacket {
    public OnJoinWorldS2CPacket(PacketByteBuf buf) {
        this(
            buf.readMap(b -> new ChunkPos(b.readVarInt(), b.readVarInt()), b -> new ChunkSummary(Objects.requireNonNull(b.readNbt()))),
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
        buf.writeMap(terrain, (b, p) -> {
            b.writeVarInt(p.x);
            b.writeVarInt(p.z);
        }, (b, s) -> {
            b.writeNbt(s.writeNbt(new NbtCompound()));
        });
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
        return SurveyorNetworking.S2C_ON_JOIN_WORLD;
    }
}
