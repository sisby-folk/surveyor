package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.SurveyorNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record WorldLoadedC2SPacket(Map<ChunkPos, BitSet> terrainBits, Map<RegistryKey<Structure>, Set<ChunkPos>> structureKeys) implements C2SPacket {
    public static WorldLoadedC2SPacket read(PacketByteBuf buf) {
        return new WorldLoadedC2SPacket(
            buf.readMap(PacketByteBuf::readChunkPos, PacketByteBuf::readBitSet),
            buf.readMap(b -> b.readRegistryKey(RegistryKeys.STRUCTURE), b -> b.readCollection(HashSet::new, PacketByteBuf::readChunkPos))
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(terrainBits, PacketByteBuf::writeChunkPos, PacketByteBuf::writeBitSet);
        buf.writeMap(structureKeys, PacketByteBuf::writeRegistryKey, (b, stars) -> b.writeCollection(stars, PacketByteBuf::writeChunkPos));
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.C2S_WORLD_LOADED;
    }
}
