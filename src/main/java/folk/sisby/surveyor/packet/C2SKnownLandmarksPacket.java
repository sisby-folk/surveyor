package folk.sisby.surveyor.packet;

import com.google.common.collect.Multimap;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.landmark.LandmarkType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record C2SKnownLandmarksPacket(Multimap<LandmarkType<?>, BlockPos> landmarks) implements C2SPacket {
    public static final Id<C2SKnownLandmarksPacket> ID = new Id<>(Identifier.of(Surveyor.ID, "c2s_known_landmarks"));
    public static final PacketCodec<PacketByteBuf, C2SKnownLandmarksPacket> CODEC = SurveyorPacketCodecs.LANDMARK_KEYS.xmap(C2SKnownLandmarksPacket::new, C2SKnownLandmarksPacket::landmarks);

    @Override
    public Id<C2SKnownLandmarksPacket> getId() {
        return ID;
    }
}
