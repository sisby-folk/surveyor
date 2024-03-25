package folk.sisby.surveyor;

import folk.sisby.surveyor.client.SurveyorClientEvents;
import folk.sisby.surveyor.packet.S2CStructuresAddedPacket;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.RegionSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
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
            ServerWorld world = player.getServerWorld();
            if (player.getServer().isHost(player.getGameProfile())) { // Singleplayer Client
                SurveyorClientEvents.Invoke.terrainUpdated(world, WorldSummary.of(world).terrain(), bitSet.stream().mapToObj(i -> RegionSummary.chunkForBit(regionPos, i)).toList());
                WorldSummary.of(world).landmarks().asMap(this).forEach((type, map) -> map.forEach((lPos, landmark) -> {
                    if (exploredLandmark(world.getRegistryKey(), landmark)) SurveyorClientEvents.Invoke.landmarksAdded(world, WorldSummary.of(world).landmarks(), landmark);
                }));
            }
            // Sync to shared players if they don't have it
        }

        @Override
        public void addChunk(RegistryKey<World> worldKey, ChunkPos pos) {
            SurveyorExploration.super.addChunk(worldKey, pos);
            ServerWorld world = player.getServerWorld();
            if (player.getServer().isHost(player.getGameProfile())) { // Singleplayer Client
                WorldTerrainSummary summary = WorldSummary.of(world).terrain();
                SurveyorClientEvents.Invoke.terrainUpdated(world, summary, pos);
                WorldSummary.of(world).landmarks().asMap(this).forEach((type, map) -> map.forEach((lPos, landmark) -> {
                    if (new ChunkPos(lPos).equals(pos)) {
                        if (exploredLandmark(world.getRegistryKey(), landmark)) SurveyorClientEvents.Invoke.landmarksAdded(world, WorldSummary.of(world).landmarks(), landmark);
                    }
                }));
            }
            // Sync to shared players if its unexplored
        }

        @Override
        public void addStructure(RegistryKey<World> worldKey, RegistryKey<Structure> structureKey, ChunkPos pos) {
            SurveyorExploration.super.addStructure(worldKey, structureKey, pos);
            ServerWorld world = player.getServerWorld();
            WorldStructureSummary summary = WorldSummary.of(world).structures();
            new S2CStructuresAddedPacket(Map.of(structureKey, Map.of(pos, summary.get(structureKey, pos))), Map.of(structureKey, summary.getType(structureKey)), MapUtil.asMultiMap(Map.of(structureKey, summary.getTags(structureKey)))).send(player);
            // Send to shared players if they don't have it
            if (player.getServer().isHost(player.getGameProfile())) { // Singleplayer Client
                SurveyorClientEvents.Invoke.structuresAdded(world, summary, structureKey, pos);
            }
        }

        @Override
        public Set<UUID> sharedPlayers() {
            return Set.of(player.getUuid());
        }
    }
}
