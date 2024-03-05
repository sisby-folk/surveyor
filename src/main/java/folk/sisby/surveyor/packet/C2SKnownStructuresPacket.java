package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.SurveyorNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record C2SKnownStructuresPacket(Map<RegistryKey<Structure>, Set<ChunkPos>> structureKeys) implements C2SPacket {
    public static C2SKnownStructuresPacket read(PacketByteBuf buf) {
        return new C2SKnownStructuresPacket(
            buf.readMap(b -> b.readRegistryKey(RegistryKeys.STRUCTURE), b -> b.readCollection(HashSet::new, PacketByteBuf::readChunkPos))
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(structureKeys, PacketByteBuf::writeRegistryKey, (b, starts) -> b.writeCollection(starts, PacketByteBuf::writeChunkPos));
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.C2S_KNOWN_STRUCTURES;
    }
}
