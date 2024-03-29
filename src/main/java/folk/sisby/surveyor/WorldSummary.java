package folk.sisby.surveyor;

import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import net.minecraft.world.World;

import java.io.File;

public record WorldSummary(WorldTerrainSummary terrain, WorldStructureSummary structures, WorldLandmarks landmarks, boolean isClient) {
    public static WorldSummary of(World world) {
        return ((SurveyorWorld) world).surveyor$getWorldSummary();
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
        Surveyor.LOGGER.info("[Surveyor] Saving data for {}", world.getRegistryKey().getValue());
        folder.mkdirs();
        terrain.save(world, folder);
        structures.save(world, folder);
        landmarks.save(world, folder);
        Surveyor.LOGGER.info("[Surveyor] Finished saving data for {}", world.getRegistryKey().getValue());
    }
}
