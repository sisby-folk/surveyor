package folk.sisby.surveyor.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import folk.sisby.surveyor.util.MapUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurveyorClientEvents {
    private static final Map<Identifier, WorldLoad> worldLoad = new HashMap<>();
    private static final Map<Identifier, TerrainUpdated> terrainUpdated = new HashMap<>();
    private static final Map<Identifier, StructuresAdded> structuresAdded = new HashMap<>();
    private static final Map<Identifier, LandmarksAdded> landmarksAdded = new HashMap<>();
    private static final Map<Identifier, LandmarksRemoved> landmarksRemoved = new HashMap<>();
    public static boolean INITIALIZING_WORLD = false;

    public static class Invoke {
        public static void worldLoad(ClientWorld world, WorldSummary summary, ClientPlayerEntity player) {
            SurveyorExploration exploration = SurveyorClient.getExploration(player);
            worldLoad.forEach((id, handler) -> handler.onWorldLoad(world, summary, player, summary.terrain().bitSet(exploration), summary.structures().keySet(exploration), summary.landmarks().keySet(exploration)));
        }

        public static void terrainUpdated(World world, WorldTerrainSummary worldTerrain, Collection<ChunkPos> chunks) {
            terrainUpdated.forEach((id, handler) -> handler.onTerrainUpdated(world, worldTerrain, chunks));
        }

        public static void terrainUpdated(World world, WorldTerrainSummary worldTerrain, ChunkPos pos) {
            terrainUpdated(world, worldTerrain, List.of(pos));
        }

        public static void structuresAdded(World world, WorldStructureSummary worldStructures, Multimap<RegistryKey<Structure>, ChunkPos> structures) {
            structuresAdded.forEach((id, handler) -> handler.onStructuresAdded(world, worldStructures, structures));
        }

        public static void structuresAdded(World world, WorldStructureSummary worldStructures, RegistryKey<Structure> key, ChunkPos pos) {
            structuresAdded(world, worldStructures, MapUtil.asMultiMap(Map.of(key, List.of(pos))));
        }

        public static void landmarksAdded(World world, WorldLandmarks worldLandmarks, Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks) {
            landmarksAdded.forEach((id, handler) -> handler.onLandmarksAdded(world, worldLandmarks, landmarks));
        }

        public static void landmarksAdded(World world, WorldLandmarks worldLandmarks, Landmark<?> landmark) {
            landmarksAdded(world, worldLandmarks, Map.of(landmark.type(), Map.of(landmark.pos(), landmark)));
        }

        public static void landmarksRemoved(World world, WorldLandmarks worldLandmarks, Multimap<LandmarkType<?>, BlockPos> landmarks) {
            landmarksRemoved.forEach((id, handler) -> handler.onLandmarksRemoved(world, worldLandmarks, landmarks));
        }

        public static void landmarksRemoved(World world, WorldLandmarks worldLandmarks, LandmarkType<?> type, BlockPos pos) {
            Multimap<LandmarkType<?>, BlockPos> map = HashMultimap.create();
            map.put(type, pos);
            landmarksRemoved(world, worldLandmarks, map);
        }
    }

    public static class Register {
        public static void worldLoad(Identifier id, WorldLoad handler) {
            worldLoad.put(id, handler);
        }

        public static void terrainUpdated(Identifier id, TerrainUpdated handler) {
            terrainUpdated.put(id, handler);
        }

        public static void structuresAdded(Identifier id, StructuresAdded handler) {
            structuresAdded.put(id, handler);
        }

        public static void landmarksAdded(Identifier id, LandmarksAdded handler) {
            landmarksAdded.put(id, handler);
        }

        public static void landmarksRemoved(Identifier id, LandmarksRemoved handler) {
            landmarksRemoved.put(id, handler);
        }
    }

    @FunctionalInterface
    public interface WorldLoad {
        void onWorldLoad(ClientWorld world, WorldSummary summary, ClientPlayerEntity player, Map<ChunkPos, BitSet> terrain, Multimap<RegistryKey<Structure>, ChunkPos> structures, Multimap<LandmarkType<?>, BlockPos> landmarks);
    }

    @FunctionalInterface
    public interface TerrainUpdated {
        void onTerrainUpdated(World world, WorldTerrainSummary worldStructures, Collection<ChunkPos> chunks);
    }

    @FunctionalInterface
    public interface StructuresAdded {
        void onStructuresAdded(World world, WorldStructureSummary worldStructures, Multimap<RegistryKey<Structure>, ChunkPos> structures);
    }

    @FunctionalInterface
    public interface LandmarksAdded {
        void onLandmarksAdded(World world, WorldLandmarks worldLandmarks, Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks);
    }

    @FunctionalInterface
    public interface LandmarksRemoved {
        void onLandmarksRemoved(World world, WorldLandmarks worldLandmarks, Multimap<LandmarkType<?>, BlockPos> landmarks);
    }
}
