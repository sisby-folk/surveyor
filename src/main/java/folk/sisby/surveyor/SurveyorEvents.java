package folk.sisby.surveyor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.structure.StructureSummary;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurveyorEvents {
    private static final Map<Identifier, WorldLoad> worldLoadHandlers = new HashMap<>();
    private static final Map<Identifier, ClientWorldLoad> clientWorldLoadHandlers = new HashMap<>();
    private static final Map<Identifier, TerrainUpdated> chunkAddedHandlers = new HashMap<>();
    private static final Map<Identifier, StructuresAdded> structureAddedHandlers = new HashMap<>();
    private static final Map<Identifier, LandmarksAdded> landmarkAddedHandlers = new HashMap<>();
    private static final Map<Identifier, LandmarksRemoved> landmarkRemovedHandlers = new HashMap<>();

    public static class Invoke {
        public static void worldLoad(ServerWorld world, WorldSummary worldSummary) {
            worldLoadHandlers.forEach((id, handler) -> handler.onWorldLoad(world, worldSummary));
        }

        public static void clientWorldLoad(World world, WorldSummary worldSummary) {
            clientWorldLoadHandlers.forEach((id, handler) -> handler.onClientWorldLoad(world, worldSummary));
        }

        public static void terrainUpdated(World world, WorldTerrainSummary worldTerrain, Collection<ChunkPos> chunks) {
            chunkAddedHandlers.forEach((id, handler) -> handler.onTerrainUpdated(world, worldTerrain, chunks));
        }

        public static void chunkUpdated(World world, WorldTerrainSummary worldTerrain, ChunkPos pos) {
            terrainUpdated(world, worldTerrain, List.of(pos));
        }

        public static void structuresAdded(World world, WorldStructureSummary worldStructures, Collection<StructureSummary> summaries) {
            structureAddedHandlers.forEach((id, handler) -> handler.onStructuresAdded(world, worldStructures, summaries));
        }

        public static void structureAdded(World world, WorldStructureSummary worldStructures, StructureSummary structureSummary) {
            structuresAdded(world, worldStructures, List.of(structureSummary));
        }

        public static void landmarksAdded(World world, WorldLandmarks worldLandmarks, Collection<Landmark<?>> landmarks) {
            landmarkAddedHandlers.forEach((id, handler) -> handler.onLandmarksAdded(world, worldLandmarks, landmarks));
        }

        public static void landmarkAdded(World world, WorldLandmarks worldLandmarks, Landmark<?> landmark) {
            landmarksAdded(world, worldLandmarks, List.of(landmark));
        }

        public static void landmarksRemoved(World world, WorldLandmarks worldLandmarks, Multimap<LandmarkType<?>, BlockPos> landmarks) {
            landmarkRemovedHandlers.forEach((id, handler) -> handler.onLandmarksRemoved(world, worldLandmarks, landmarks));
        }

        public static void landmarkRemoved(World world, WorldLandmarks worldLandmarks, LandmarkType<?> type, BlockPos pos) {
            Multimap<LandmarkType<?>, BlockPos> map = HashMultimap.create();
            map.put(type, pos);
            landmarksRemoved(world, worldLandmarks, map);
        }
    }

    public static class Register {
        public static void worldLoad(Identifier id, WorldLoad handler) {
            worldLoadHandlers.put(id, handler);
        }

        public static void clientWorldLoad(Identifier id, ClientWorldLoad handler) {
            clientWorldLoadHandlers.put(id, handler);
        }

        public static void terrainUpdated(Identifier id, TerrainUpdated handler) {
            chunkAddedHandlers.put(id, handler);
        }

        public static void structuresAdded(Identifier id, StructuresAdded handler) {
            structureAddedHandlers.put(id, handler);
        }

        public static void landmarksAdded(Identifier id, LandmarksAdded handler) {
            landmarkAddedHandlers.put(id, handler);
        }

        public static void landmarksRemoved(Identifier id, LandmarksRemoved handler) {
            landmarkRemovedHandlers.put(id, handler);
        }
    }

    @FunctionalInterface
    public interface WorldLoad {
        void onWorldLoad(ServerWorld world, WorldSummary worldSummary);
    }

    @FunctionalInterface
    public interface ClientWorldLoad {
        void onClientWorldLoad(World world, WorldSummary worldSummary);
    }

    @FunctionalInterface
    public interface TerrainUpdated {
        void onTerrainUpdated(World world, WorldTerrainSummary worldStructures, Collection<ChunkPos> chunks);
    }

    @FunctionalInterface
    public interface StructuresAdded {
        void onStructuresAdded(World world, WorldStructureSummary worldTerrain, Collection<StructureSummary> summaries);
    }

    @FunctionalInterface
    public interface LandmarksAdded {
        void onLandmarksAdded(World world, WorldLandmarks worldLandmarks, Collection<Landmark<?>> landmarks);
    }

    @FunctionalInterface
    public interface LandmarksRemoved {
        void onLandmarksRemoved(World world, WorldLandmarks worldLandmarks, Multimap<LandmarkType<?>, BlockPos> landmarks);
    }
}
