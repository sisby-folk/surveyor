package folk.sisby.surveyor.packet;

import com.google.common.collect.Multimap;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public record SyncLandmarksAddedPacket(Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks) implements SyncPacket {
    public static final CustomPayload.Id<SyncLandmarksAddedPacket> ID = new CustomPayload.Id<>(Identifier.of(Surveyor.ID, "landmarks_added"));
    public static final PacketCodec<ByteBuf, SyncLandmarksAddedPacket> CODEC = SurveyorPacketCodecs.LANDMARK_SUMMARIES.xmap(SyncLandmarksAddedPacket::new, SyncLandmarksAddedPacket::landmarks);

    public static SyncLandmarksAddedPacket of(Multimap<LandmarkType<?>, BlockPos> keySet, WorldLandmarks summary) {
        return summary.createUpdatePacket(keySet);
    }

    @Override
    public Id<SyncLandmarksAddedPacket> getId() {
        return ID;
    }
}
