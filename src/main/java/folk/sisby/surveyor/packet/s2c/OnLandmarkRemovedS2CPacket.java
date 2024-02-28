package folk.sisby.surveyor.packet.s2c;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.Landmarks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OnLandmarkRemovedS2CPacket(LandmarkType<?> type, BlockPos pos) implements S2CPacket {
    public OnLandmarkRemovedS2CPacket(PacketByteBuf buf) {
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
        return SurveyorNetworking.S2C_ON_LANDMARK_REMOVED;
    }
}
