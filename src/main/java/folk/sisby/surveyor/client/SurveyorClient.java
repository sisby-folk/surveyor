package folk.sisby.surveyor.client;

import folk.sisby.surveyor.WorldSummary;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;

public class SurveyorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SurveyorClientNetworking.init();
        ClientChunkEvents.CHUNK_LOAD.register(WorldSummary::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(WorldSummary::onChunkUnload);
    }
}
