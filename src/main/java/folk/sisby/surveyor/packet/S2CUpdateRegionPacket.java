package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.RegionSummary;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ChunkPos;

import java.util.BitSet;
import java.util.List;

public record S2CUpdateRegionPacket(boolean shared, ChunkPos regionPos, List<Integer> biomePalette, List<Integer> blockPalette, BitSet set, List<ChunkSummary> chunks) implements S2CPacket {
    public static final CustomPayload.Id<S2CUpdateRegionPacket> ID = new CustomPayload.Id<>(Identifier.of(Surveyor.ID, "s2c_update_region"));
    public static final PacketCodec<RegistryByteBuf, S2CUpdateRegionPacket> CODEC = PacketCodec.tuple(
        PacketCodecs.BOOL, S2CUpdateRegionPacket::shared,
        PacketCodecs.VAR_LONG.xmap(ChunkPos::new, ChunkPos::toLong), S2CUpdateRegionPacket::regionPos,
        PacketCodecs.INTEGER.collect(PacketCodecs.toList()), S2CUpdateRegionPacket::biomePalette,
        PacketCodecs.INTEGER.collect(PacketCodecs.toList()), S2CUpdateRegionPacket::blockPalette,
        PacketCodecs.codec(Codecs.BIT_SET), S2CUpdateRegionPacket::set,
        PacketCodec.of(ChunkSummary::writeBuf, ChunkSummary::new).collect(PacketCodecs.toList()), S2CUpdateRegionPacket::chunks,
        S2CUpdateRegionPacket::new
    );

    public static S2CUpdateRegionPacket of(boolean shared, ChunkPos regionPos, RegionSummary summary, BitSet keys) {
        return summary.createUpdatePacket(shared, regionPos, keys);
    }

    @Override
    public CustomPayload.Id<S2CUpdateRegionPacket> getId() {
        return ID;
    }
}
