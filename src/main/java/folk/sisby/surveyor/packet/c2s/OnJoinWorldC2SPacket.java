package folk.sisby.surveyor.packet.c2s;

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

public record OnJoinWorldC2SPacket(Set<ChunkPos> terrainKeys, Map<RegistryKey<Structure>, Set<ChunkPos>> structureKeys) implements C2SPacket {
    public OnJoinWorldC2SPacket(PacketByteBuf buf) {
        this(
            buf.readCollection(HashSet::new, b -> new ChunkPos(b.readVarInt(), b.readVarInt())),
            buf.readMap(b -> RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(b.readString())), b -> b.readCollection(HashSet::new, b2 -> new ChunkPos(b2.readVarInt(), b2.readVarInt())))
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeCollection(terrainKeys, (b, pos) -> {
            b.writeVarInt(pos.x);
            b.writeVarInt(pos.z);
        });
        buf.writeMap(structureKeys, (b, s) -> {
            b.writeString(s.toString());
        }, (b, stars) -> {
            b.writeCollection(stars, (b2, pos) -> {
                b.writeVarInt(pos.x);
                b.writeVarInt(pos.z);
            });
        });
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.C2S_ON_JOIN_WORLD;
    }
}
