package folk.sisby.surveyor.packet.c2s;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.Landmarks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OnLandmarkRemovedC2SPacket(LandmarkType<?> type, BlockPos pos) implements C2SPacket {
    public OnLandmarkRemovedC2SPacket(PacketByteBuf buf) {
        this(
            Landmarks.getType(buf.readIdentifier()),
            buf.readBlockPos()
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeIdentifier(type.id());
        buf.writeBlockPos(pos);
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.C2S_ON_LANDMARK_REMOVED;
    }
}
