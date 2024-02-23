package folk.sisby.surveyor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Surveyor implements ModInitializer {
    public static final String ID = "surveyor";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final String DATA_SUBFOLDER = "data";
    public static final SurveyorConfig CONFIG = SurveyorConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", "surveyor", SurveyorConfig.class);

    public static File getSavePath(ServerWorld world) {
        return world.getServer().getSavePath(WorldSavePath.ROOT).resolve(DATA_SUBFOLDER).resolve(Surveyor.ID).toFile();
    }

    @Override
    public void onInitialize() {
        SurveyorNetworking.init();
        ServerChunkEvents.CHUNK_LOAD.register(WorldSummary::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(WorldSummary::onChunkUnload);
    }
}
