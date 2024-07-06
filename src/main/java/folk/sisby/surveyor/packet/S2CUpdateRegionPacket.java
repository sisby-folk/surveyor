package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.terrain.RegionSummary;
import folk.sisby.surveyor.util.BitSetUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

public record S2CUpdateRegionPacket(boolean shared, ChunkPos regionPos, RegionSummary summary, BitSet chunks) implements S2CPacket {
    public static final Identifier ID = new Identifier(Surveyor.ID, "s2c_update_region");

    public static S2CUpdateRegionPacket handle(PacketByteBuf buf, WorldSummary summary) {
        boolean shared = buf.readBoolean();
        ChunkPos regionPos = buf.readChunkPos();
        if (summary.terrain() == null) return new S2CUpdateRegionPacket(shared, regionPos, null, new BitSet());
        RegionSummary region = summary.terrain().getRegion(regionPos);
        BitSet chunks = region.readBuf(buf);
        return new S2CUpdateRegionPacket(
            shared,
            regionPos,
            region,
            chunks
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeBoolean(shared);
        buf.writeChunkPos(regionPos);
        summary.writeBuf(buf, chunks);
    }

    @Override
    public Collection<PacketByteBuf> toBufs() {
        List<PacketByteBuf> bufs = new ArrayList<>();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        writeBuf(buf);
        if (buf.readableBytes() < MAX_PAYLOAD_SIZE) {
            bufs.add(buf);
        } else {
            if (chunks.cardinality() == 1) {
                int bit = chunks.stream().findFirst().orElseThrow();
                Surveyor.LOGGER.error("Couldn't create a terrain update packet at {} - an individual chunk would be too large to send!", "[%d,%d]".formatted(regionPos.x + RegionSummary.xForBit(bit), regionPos.z + RegionSummary.zForBit(bit)));
                return List.of();
            }
            for (BitSet splitChunks : BitSetUtil.half(chunks)) {
                bufs.addAll(new S2CUpdateRegionPacket(shared, regionPos, summary, splitChunks).toBufs());
            }
        }
        return bufs;
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
