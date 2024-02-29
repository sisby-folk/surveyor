package folk.sisby.surveyor;

import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import net.minecraft.world.World;

import java.io.File;

public record WorldSummary(WorldTerrainSummary terrain, WorldStructureSummary structures, WorldLandmarks landmarks) {
    public enum Type {
        SERVER,
        CLIENT
    }

    public static WorldSummary load(Type type, World world, File folder) {
        Surveyor.LOGGER.info("[Surveyor] Loading data for {}", world.getRegistryKey().getValue());
        folder.mkdirs();
        WorldTerrainSummary terrain = WorldTerrainSummary.load(world, folder, type);
        WorldStructureSummary structures = WorldStructureSummary.load(world, folder);
        WorldLandmarks landmarks = WorldLandmarks.load(world, folder);
        Surveyor.LOGGER.info("[Surveyor] Finished loading data for {}", world.getRegistryKey().getValue());
        return new WorldSummary(terrain, structures, landmarks);
    }

    public void save(World world, File folder) {
        Surveyor.LOGGER.info("[Surveyor] Saving data for {}", world.getRegistryKey().getValue());
        folder.mkdirs();
        terrain.save(world, folder);
        structures.save(world, folder);
        landmarks.save(world, folder);
        Surveyor.LOGGER.info("[Surveyor] Finished saving data for {}", world.getRegistryKey().getValue());
    }
}
