package folk.sisby.surveyor;

import folk.sisby.surveyor.chunk.ChunkSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.structure.StructureSummary;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class SurveyorEvents {
    private static final Map<Identifier, WorldLoad> worldLoadHandlers = new HashMap<>();
    private static final Map<Identifier, ClientWorldLoad> clientWorldLoadHandlers = new HashMap<>();
    private static final Map<Identifier, ChunkAdded> chunkAddedHandlers = new HashMap<>();
    private static final Map<Identifier, StructureAdded> structureAddedHandlers = new HashMap<>();
    private static final Map<Identifier, LandmarkAdded> landmarkAddedHandlers = new HashMap<>();
    private static final Map<Identifier, LandmarkRemoved> landmarkRemovedHandlers = new HashMap<>();

    public static class Invoke {
        public static void worldLoad(ServerWorld world, WorldSummary worldSummary) {
            worldLoadHandlers.forEach((id, handler) -> handler.onWorldLoad(world, worldSummary));
        }

        public static void clientWorldLoad(World world, WorldSummary worldSummary) {
            clientWorldLoadHandlers.forEach((id, handler) -> handler.onClientWorldLoad(world, worldSummary));
        }

        public static void chunkAdded(World world, WorldSummary worldSummary, ChunkPos pos, ChunkSummary chunkSummary) {
            chunkAddedHandlers.forEach((id, handler) -> handler.onChunkAdded(world, worldSummary, pos, chunkSummary));
        }

        public static void structureAdded(World world, WorldSummary worldSummary, StructureSummary structureSummary) {
            structureAddedHandlers.forEach((id, handler) -> handler.onStructureAdded(world, worldSummary, structureSummary));
        }

        public static void landmarkAdded(World world, WorldSummary worldSummary, Landmark<?> landmark) {
            landmarkAddedHandlers.forEach((id, handler) -> handler.onLandmarkAdded(world, worldSummary, landmark));
        }

        public static void landmarkRemoved(World world, WorldSummary worldSummary, LandmarkType<?> type, BlockPos pos) {
            landmarkRemovedHandlers.forEach((id, handler) -> handler.onLandmarkRemoved(world, worldSummary, type, pos));
        }
    }

    public static class Register {
        public static void worldLoad(Identifier id, WorldLoad handler) {
            worldLoadHandlers.put(id, handler);
        }

        public static void clientWorldLoad(Identifier id, ClientWorldLoad handler) {
            clientWorldLoadHandlers.put(id, handler);
        }

        public static void chunkAdded(Identifier id, ChunkAdded handler) {
            chunkAddedHandlers.put(id, handler);
        }

        public static void structureAdded(Identifier id, StructureAdded handler) {
            structureAddedHandlers.put(id, handler);
        }

        public static void landmarkAdded(Identifier id, LandmarkAdded handler) {
            landmarkAddedHandlers.put(id, handler);
        }

        public static void landmarkRemoved(Identifier id, LandmarkRemoved handler) {
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
    public interface ChunkAdded {
        void onChunkAdded(World world, WorldSummary worldSummary, ChunkPos pos, ChunkSummary chunkSummary);
    }

    @FunctionalInterface
    public interface StructureAdded {
        void onStructureAdded(World world, WorldSummary worldSummary, StructureSummary structureSummary);
    }

    @FunctionalInterface
    public interface LandmarkAdded {
        void onLandmarkAdded(World world, WorldSummary worldSummary, Landmark<?> landmark);
    }

    @FunctionalInterface
    public interface LandmarkRemoved {
        void onLandmarkRemoved(World world, WorldSummary worldSummary, LandmarkType<?> type, BlockPos pos);
    }
}
