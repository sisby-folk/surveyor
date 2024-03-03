package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.terrain.ChunkSummary;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.util.Map;
import java.util.Objects;

public record TerrainAddedS2CPacket(Map<ChunkPos, ChunkSummary> terrain) implements S2CPacket {
    public static TerrainAddedS2CPacket read(PacketByteBuf buf) {
        return new TerrainAddedS2CPacket(
            buf.readMap(b -> new ChunkPos(b.readVarInt(), b.readVarInt()), b -> new ChunkSummary(Objects.requireNonNull(b.readNbt())))
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
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.S2C_TERRAIN_ADDED;
    }
}
