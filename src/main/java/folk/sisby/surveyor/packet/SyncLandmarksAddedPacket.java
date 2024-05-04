package folk.sisby.surveyor.packet;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.util.MapUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public record SyncLandmarksAddedPacket(Multimap<LandmarkType<?>, BlockPos> keySet, WorldLandmarks summary) implements SyncPacket {
    public static final Identifier ID = new Identifier(Surveyor.ID, "landmarks_added");

    public static SyncLandmarksAddedPacket of(Landmark<?> landmark, WorldLandmarks summary) {
        return new SyncLandmarksAddedPacket(MapUtil.asMultiMap(Map.of(landmark.type(), List.of(landmark.pos()))), summary);
    }

    public static SyncLandmarksAddedPacket handle(PacketByteBuf buf, World world, WorldSummary summary, ServerPlayerEntity sender) {
        Multimap<LandmarkType<?>, BlockPos> keySet = summary.landmarks().readBuf(world, buf, sender);
        return new SyncLandmarksAddedPacket(
            keySet,
            summary.landmarks()
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        summary.writeBuf(buf, keySet);
    }

    @Override
    public Collection<PacketByteBuf> toBufs() {
        List<PacketByteBuf> bufs = new ArrayList<>();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        writeBuf(buf);
        if (buf.readableBytes() < MAX_PAYLOAD_SIZE) {
            bufs.add(buf);
        } else {
            if (keySet.size() == 1) throw new RuntimeException("Couldn't create a landmark update packet - an individual landmark would be too large to send!");
            Multimap<LandmarkType<?>, BlockPos> firstHalf = HashMultimap.create();
            Multimap<LandmarkType<?>, BlockPos> secondHalf = HashMultimap.create();
            keySet.forEach((key, pos) -> {
                if (firstHalf.size() < keySet.size() / 2) {
                    firstHalf.put(key, pos);
                } else {
                    secondHalf.put(key, pos);
                }
            });
            bufs.addAll(new SyncLandmarksAddedPacket(firstHalf, summary).toBufs());
            bufs.addAll(new SyncLandmarksAddedPacket(secondHalf, summary).toBufs());
        }
        return bufs;
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
