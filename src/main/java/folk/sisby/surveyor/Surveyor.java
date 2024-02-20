package folk.sisby.surveyor;

import folk.sisby.surveyor.network.SurveyorNetworking;
import folk.sisby.surveyor.structure.StructureSummaryState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Surveyor implements ModInitializer {
    public static final String ID = "surveyor";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final SurveyorConfig CONFIG = SurveyorConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", "surveyor", SurveyorConfig.class);

    @Override
    public void onInitialize() {
        SurveyorNetworking.init();

        ServerWorldEvents.LOAD.register((s, world) -> ((SurveyorWorld) world).surveyor$getWorldSummary());
        ServerWorldEvents.LOAD.register((s, world) -> StructureSummaryState.getOrCreate(world));

        ServerChunkEvents.CHUNK_LOAD.register(WorldSummary::onChunkLoad);
        ServerChunkEvents.CHUNK_LOAD.register(StructureSummaryState::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(WorldSummary::onChunkUnload);
    }
}
