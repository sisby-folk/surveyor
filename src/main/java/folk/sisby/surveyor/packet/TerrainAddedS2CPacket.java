package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.terrain.ChunkSummary;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record TerrainAddedS2CPacket(Map<Triple<ChunkPos, List<Integer>, List<Integer>>, Map<ChunkPos, ChunkSummary>> terrain) implements S2CPacket {
    public static TerrainAddedS2CPacket read(PacketByteBuf buf) {
        return new TerrainAddedS2CPacket(buf.readMap(
            b -> Triple.of(b.readChunkPos(), b.readCollection(ArrayList::new, PacketByteBuf::readVarInt), b.readCollection(ArrayList::new, PacketByteBuf::readVarInt)),
            b -> b.readMap(b2 -> new ChunkPos(b2.readVarInt(), b2.readVarInt()), b2 -> new ChunkSummary(Objects.requireNonNull(b2.readNbt())))
        ));
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(terrain,
            (b, t) -> {
                b.writeChunkPos(t.getLeft());
                b.writeCollection(t.getMiddle(), PacketByteBuf::writeVarInt);
                b.writeCollection(t.getRight(), PacketByteBuf::writeVarInt);
            },
            (b, m) -> b.writeMap(m,
                (b2, p) -> {
                    b2.writeVarInt(p.x);
                    b2.writeVarInt(p.z);
                },
                (b2, s) -> {
                    b2.writeNbt(s.writeNbt(new NbtCompound()));
                }
            )
        );
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.S2C_TERRAIN_ADDED;
    }
}
