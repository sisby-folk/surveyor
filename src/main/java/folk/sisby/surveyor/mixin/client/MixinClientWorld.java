package folk.sisby.surveyor.mixin.client;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements SurveyorWorld {
	@Unique
	private WorldSummary surveyor$summary = null;

	@Override
	public WorldSummary surveyor$getSummary() {
		ClientWorld self = (ClientWorld) (Object) this;
		if (surveyor$summary == null) {
			if (MinecraftClient.getInstance().isIntegratedServerRunning()) {
				surveyor$summary = WorldSummary.of(SurveyorClient.stealServerWorld(self.getRegistryKey()));
			} else {
				surveyor$summary = WorldSummary.load(self, SurveyorClient.getWorldSavePath(self), true);
			}
		}
		return surveyor$summary;
	}

	@Inject(method = "disconnect", at = @At("HEAD"))
	public void saveOnDisconnect(CallbackInfo ci) {
		ClientWorld self = (ClientWorld) (Object) this;
		WorldSummary summary = WorldSummary.of(self);
		if (summary.isClient()) summary.save(self, SurveyorClient.getWorldSavePath(self), false);
	}
}
