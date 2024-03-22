package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.Landmarks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record SyncLandmarksRemovedPacket(Map<LandmarkType<?>, Collection<BlockPos>> landmarks) implements SyncPacket {
    public static final Identifier ID = new Identifier(Surveyor.ID, "landmarks_removed");

    public static SyncLandmarksRemovedPacket of(LandmarkType<?> type, BlockPos pos) {
        return new SyncLandmarksRemovedPacket(Map.of(type, List.of(pos)));
    }

    public static SyncLandmarksRemovedPacket read(PacketByteBuf buf) {
        return new SyncLandmarksRemovedPacket(buf.readMap(
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
        return ID;
    }
}
