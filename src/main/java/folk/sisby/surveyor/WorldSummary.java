package folk.sisby.surveyor;

import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public record WorldSummary(@Nullable WorldTerrainSummary terrain, @Nullable WorldStructureSummary structures, @Nullable WorldLandmarks landmarks, boolean isClient) {
    private static boolean ENABLE_TERRAIN = false;
    private static boolean ENABLE_STRUCTURES = false;
    private static boolean ENABLE_LANDMARKS = false;

    public static WorldSummary of(World world) {
        return ((SurveyorWorld) world).surveyor$getSummary();
    }
    
    public static WorldSummary load(World world, File folder, boolean isClient) {
        Surveyor.LOGGER.info("[Surveyor] Loading data for {}", world.getRegistryKey().getValue());
        folder.mkdirs();
        WorldTerrainSummary terrain = (Surveyor.CONFIG.terrain == SurveyorConfig.SystemMode.DISABLED || Surveyor.CONFIG.terrain == SurveyorConfig.SystemMode.DYNAMIC && !ENABLE_TERRAIN && isClient) ? null : WorldTerrainSummary.load(world, folder);
        WorldStructureSummary structures =  (Surveyor.CONFIG.structures == SurveyorConfig.SystemMode.DISABLED || Surveyor.CONFIG.structures == SurveyorConfig.SystemMode.DYNAMIC && !ENABLE_STRUCTURES && isClient) ? null : WorldStructureSummary.load(world, folder);
        WorldLandmarks landmarks =  (Surveyor.CONFIG.landmarks == SurveyorConfig.SystemMode.DISABLED || Surveyor.CONFIG.landmarks == SurveyorConfig.SystemMode.DYNAMIC && !ENABLE_LANDMARKS && isClient) ? null : WorldLandmarks.load(world, folder);
        Surveyor.LOGGER.info("[Surveyor] Finished loading data for {}", world.getRegistryKey().getValue());
        return new WorldSummary(terrain, structures, landmarks, isClient);
    }

    public void save(World world, File folder, boolean suppressLogs) {
        if (terrain == null && structures == null && landmarks == null) return;
        folder.mkdirs();
        int chunks = terrain == null ? 0 : terrain.save(world, folder);
        int keys = structures == null ? 0 : structures.save(world, folder);
        int marks = landmarks == null ? 0 : landmarks.save(world, folder);
        if (!suppressLogs && (chunks > 0 || keys > 0 || marks > 0)) Surveyor.LOGGER.info("[Surveyor] Finished saving data for {} | cleaned {} terrain regions, {} structure regions, {} landmarks", world.getRegistryKey().getValue(), chunks, keys, marks);
    }

    public static void enableTerrain() {
        ENABLE_TERRAIN = true;
    }

    public static void enableStructures() {
        ENABLE_STRUCTURES = true;
    }

    public static void enableLandmarks() {
        ENABLE_LANDMARKS = true;
    }
}
