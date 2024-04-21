package folk.sisby.surveyor.packet;

import com.google.common.collect.Multimap;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.util.MapUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.Map;
import java.util.Set;

public record S2CStructuresAddedPacket(boolean shared, Multimap<RegistryKey<Structure>, ChunkPos> keySet, WorldStructureSummary summary) implements S2CPacket {
    public static final Identifier ID = new Identifier(Surveyor.ID, "s2c_structures_added");

    public static S2CStructuresAddedPacket of(boolean shared, RegistryKey<Structure> key, ChunkPos pos, WorldStructureSummary summary) {
        return new S2CStructuresAddedPacket(shared, MapUtil.asMultiMap(Map.of(key, Set.of(pos))), summary);
    }

    public static S2CStructuresAddedPacket handle(PacketByteBuf buf, World world, WorldSummary summary) {
        boolean shared = buf.readBoolean();
        Multimap<RegistryKey<Structure>, ChunkPos> keySet = summary.structures().readBuf(world, buf);
        return new S2CStructuresAddedPacket(
            shared,
            keySet,
            summary.structures()
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeBoolean(shared);
        summary.writeBuf(buf, keySet);
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
