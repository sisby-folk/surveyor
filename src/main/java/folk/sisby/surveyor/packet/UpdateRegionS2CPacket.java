package folk.sisby.surveyor.packet;

import com.google.common.collect.Lists;
import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.terrain.RegionSummary;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record UpdateRegionS2CPacket(ChunkPos regionPos, RegionSummary summary, Set<ChunkPos> chunks) implements S2CPacket {
    public static UpdateRegionS2CPacket handle(PacketByteBuf buf, DynamicRegistryManager manager, WorldSummary summary) {
        ChunkPos regionPos = buf.readChunkPos();
        RegionSummary region = summary.terrain().getRegion(regionPos);
        Set<ChunkPos> chunks = region.readBuf(manager, buf);
        return new UpdateRegionS2CPacket(
            regionPos,
            region,
            chunks
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeChunkPos(regionPos);
        summary.writeBuf(buf, chunks);
    }

    @Override
    public Collection<PacketByteBuf> toBufs() {
        List<PacketByteBuf> bufs = new ArrayList<>();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        writeBuf(buf);
        if (buf.readableBytes() < 1_900_000) {
            bufs.add(buf);
        } else {
            if (chunks.size() == 1) throw new RuntimeException("Couldn't create a terrain update packet - an individual chunk would be too large to send!");
            for (List<ChunkPos> splitChunks : Lists.partition(chunks.stream().toList(), (int) Math.ceil(chunks.size() / 2.0))) {
                bufs.addAll(new UpdateRegionS2CPacket(regionPos, summary, new HashSet<>(splitChunks)).toBufs());
            }
        }
        return bufs;
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.S2C_UPDATE_REGION;
    }
}
