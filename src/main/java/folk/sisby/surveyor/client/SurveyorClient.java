package folk.sisby.surveyor.client;

import net.fabricmc.api.ClientModInitializer;

public class SurveyorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SurveyorClientNetworking.init();
    }
}
