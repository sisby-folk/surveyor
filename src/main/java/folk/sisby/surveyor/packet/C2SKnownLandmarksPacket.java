package folk.sisby.surveyor.packet;

import com.google.common.collect.Multimap;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.Landmarks;
import folk.sisby.surveyor.util.MapUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record C2SKnownLandmarksPacket(Multimap<LandmarkType<?>, BlockPos> landmarks) implements C2SPacket {
    public static final Identifier ID = new Identifier(Surveyor.ID, "c2s_known_landmarks");

    public static C2SKnownLandmarksPacket read(PacketByteBuf buf) {
        return new C2SKnownLandmarksPacket(MapUtil.asMultiMap(buf.readMap(
            b -> Landmarks.getType(b.readIdentifier()),
            b -> b.readList(PacketByteBuf::readBlockPos)
        )));
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(landmarks.asMap(),
            (b, k) -> b.writeIdentifier(k.id()),
            (b, c) -> b.writeCollection(c, PacketByteBuf::writeBlockPos)
        );
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
