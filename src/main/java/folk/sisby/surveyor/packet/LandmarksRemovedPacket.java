package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.Landmarks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record LandmarksRemovedPacket(Map<LandmarkType<?>, Collection<BlockPos>> landmarks) implements S2CPacket, C2SPacket {
    public static LandmarksRemovedPacket of(LandmarkType<?> type, BlockPos pos) {
        return new LandmarksRemovedPacket(Map.of(type, List.of(pos)));
    }

    public static LandmarksRemovedPacket read(PacketByteBuf buf) {
        return new LandmarksRemovedPacket(buf.readMap(
            b -> Landmarks.getType(b.readIdentifier()),
            b -> b.readList(PacketByteBuf::readBlockPos)
        ));
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(landmarks,
            (b, k) -> b.writeIdentifier(k.id()),
            (b, c) -> b.writeCollection(c, PacketByteBuf::writeBlockPos)
        );
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.LANDMARKS_REMOVED;
    }
}
