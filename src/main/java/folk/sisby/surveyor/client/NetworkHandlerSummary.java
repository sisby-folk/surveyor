package folk.sisby.surveyor.client;

import folk.sisby.surveyor.PlayerSummary;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NetworkHandlerSummary {
	private final ClientPlayNetworkHandler handler;
	private final Map<UUID, PlayerSummary> offlineSummaries;

	public NetworkHandlerSummary(ClientPlayNetworkHandler handler) {
		this.handler = handler;
		this.offlineSummaries = new HashMap<>();
	}

	public static NetworkHandlerSummary of(ClientPlayNetworkHandler handler) {
		return ((SurveyorNetworkHandler) handler).surveyor$getSummary();
	}

	public void mergeSummaries(Map<UUID, PlayerSummary> summaries) {
		offlineSummaries.putAll(summaries);
	}

	public PlayerSummary getPlayer(UUID uuid) {
		PlayerEntity player = handler.getWorld().getPlayerByUuid(uuid);
		if (player != null) {
			return new PlayerSummary.PlayerEntitySummary(player);
		} else {
			return offlineSummaries.get(uuid);
		}
	}
}
