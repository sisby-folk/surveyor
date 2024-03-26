package folk.sisby.surveyor;

import folk.sisby.surveyor.packet.S2CStructuresAddedPacket;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.util.MapUtil;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface SurveyorPlayer {
    SurveyorExploration surveyor$getExploration();

    int surveyor$getViewDistance();

    record ServerPlayerExploration(ServerPlayerEntity player, Map<RegistryKey<World>, Map<ChunkPos, BitSet>> terrain, Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> structures) implements SurveyorExploration {
        public static final String KEY_DATA = "surveyor";

        @Override
        public void mergeRegion(RegistryKey<World> worldKey, ChunkPos regionPos, BitSet bitSet) {
            SurveyorExploration.super.mergeRegion(worldKey, regionPos, bitSet);
            if (player.getServer().isHost(player.getGameProfile())) updateClientForMergeRegion(player.getServerWorld(), regionPos, bitSet);
            // Sync to shared players if they don't have it
        }

        @Override
        public void addChunk(RegistryKey<World> worldKey, ChunkPos pos) {
            SurveyorExploration.super.addChunk(worldKey, pos);
            if (player.getServer().isHost(player.getGameProfile())) updateClientForAddChunk(player.getServerWorld(), pos);
            // Sync to shared players if its unexplored
        }

        @Override
        public void addStructure(RegistryKey<World> worldKey, RegistryKey<Structure> structureKey, ChunkPos pos) {
            SurveyorExploration.super.addStructure(worldKey, structureKey, pos);
            ServerWorld world = player.getServerWorld();
            if (player.getServer().isHost(player.getGameProfile())) updateClientForAddStructure(world, structureKey, pos);
            WorldStructureSummary summary = WorldSummary.of(world).structures();
            new S2CStructuresAddedPacket(Map.of(structureKey, Map.of(pos, summary.get(structureKey, pos))), Map.of(structureKey, summary.getType(structureKey)), MapUtil.asMultiMap(Map.of(structureKey, summary.getTags(structureKey)))).send(player);
            // Send to shared players if they don't have it
        }

        @Override
        public Set<UUID> sharedPlayers() {
            return Set.of(player.getUuid());
        }
    }
}
