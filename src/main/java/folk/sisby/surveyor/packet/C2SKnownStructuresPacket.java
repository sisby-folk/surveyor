package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.Surveyor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record C2SKnownStructuresPacket(Map<RegistryKey<Structure>, Set<ChunkPos>> structureKeys) implements C2SPacket {
    public static final Identifier ID = new Identifier(Surveyor.ID, "c2s_known_structures");

    public static C2SKnownStructuresPacket read(PacketByteBuf buf) {
        return new C2SKnownStructuresPacket(
            buf.readMap(b -> b.readRegistryKey(RegistryKeys.STRUCTURE), b -> new HashSet<>(Arrays.stream(b.readLongArray()).mapToObj(ChunkPos::new).toList()))
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(structureKeys, PacketByteBuf::writeRegistryKey, (b, starts) -> b.writeLongArray(starts.stream().mapToLong(ChunkPos::toLong).toArray()));
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
