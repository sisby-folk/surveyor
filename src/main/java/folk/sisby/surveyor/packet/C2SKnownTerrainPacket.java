package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.SurveyorNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.util.BitSet;
import java.util.Map;

public record C2SKnownTerrainPacket(Map<ChunkPos, BitSet> terrainBits) implements C2SPacket {
    public static C2SKnownTerrainPacket read(PacketByteBuf buf) {
        return new C2SKnownTerrainPacket(
            buf.readMap(PacketByteBuf::readChunkPos, PacketByteBuf::readBitSet)
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(terrainBits, PacketByteBuf::writeChunkPos, PacketByteBuf::writeBitSet);
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.C2S_KNOWN_TERRAIN;
    }
}
