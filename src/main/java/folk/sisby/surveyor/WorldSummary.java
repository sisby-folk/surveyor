package folk.sisby.surveyor;

import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import net.minecraft.world.World;

import java.io.File;

public record WorldSummary(WorldTerrainSummary terrain, WorldStructureSummary structures, WorldLandmarks landmarks, boolean isClient) {
    public static WorldSummary of(World world) {
        return ((SurveyorWorld) world).surveyor$getSummary();
    }
    
    public static WorldSummary load(World world, File folder, boolean isClient) {
        Surveyor.LOGGER.info("[Surveyor] Loading data for {}", world.getRegistryKey().getValue());
        folder.mkdirs();
        WorldTerrainSummary terrain = WorldTerrainSummary.load(world, folder);
        WorldStructureSummary structures = WorldStructureSummary.load(world, folder);
        WorldLandmarks landmarks = WorldLandmarks.load(world, folder);
        Surveyor.LOGGER.info("[Surveyor] Finished loading data for {}", world.getRegistryKey().getValue());
        return new WorldSummary(terrain, structures, landmarks, isClient);
    }

    public void save(World world, File folder, boolean suppressLogs) {
        folder.mkdirs();
        int chunks = terrain.save(world, folder);
        int keys = structures.save(world, folder);
        int marks = landmarks.save(world, folder);
        if (!suppressLogs && (chunks > 0 || keys > 0 || marks > 0)) Surveyor.LOGGER.info("[Surveyor] Finished saving data for {} | cleaned {} terrain regions, {} structure regions, {} landmarks", world.getRegistryKey().getValue(), chunks, keys, marks);
    }
}
