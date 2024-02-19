package folk.sisby.surveyor.client;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.chunk.ChunkSummaryState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class SurveyorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SurveyorClientNetworking.init();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> ((SurveyorWorld) client.world).surveyor$getChunkSummaryState()));

        ClientChunkEvents.CHUNK_LOAD.register(ChunkSummaryState::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(ChunkSummaryState::onChunkUnload);
    }
}
