package folk.sisby.surveyor.client;

import com.mojang.authlib.GameProfile;

public interface SurveyorNetworkHandler {
	NetworkHandlerSummary surveyor$getSummary();

	GameProfile getProfile();
}
