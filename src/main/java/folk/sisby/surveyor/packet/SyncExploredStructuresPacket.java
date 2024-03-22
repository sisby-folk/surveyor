package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.Surveyor;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.Map;

public record SyncExploredStructuresPacket(RegistryKey<World> worldKey, Map<RegistryKey<Structure>, LongSet> structureKeys) implements SyncPacket {
    public static final Identifier ID = new Identifier(Surveyor.ID, "explored_structures");

    public static SyncExploredStructuresPacket read(PacketByteBuf buf) {
        return new SyncExploredStructuresPacket(
            buf.readRegistryKey(RegistryKeys.WORLD),
            buf.readMap(b -> b.readRegistryKey(RegistryKeys.STRUCTURE), b -> new LongOpenHashSet(b.readLongArray()))
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeRegistryKey(worldKey);
        buf.writeMap(structureKeys, PacketByteBuf::writeRegistryKey, (b, starts) -> b.writeLongArray(starts.toLongArray()));
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
