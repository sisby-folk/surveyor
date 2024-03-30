package folk.sisby.surveyor.mixin.client;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements SurveyorWorld {
    @Unique
    private WorldSummary surveyor$worldSummary = null;

    @Override
    public WorldSummary surveyor$getWorldSummary() {
        ClientWorld self = (ClientWorld) (Object) this;
        if (surveyor$worldSummary == null) {
            if (MinecraftClient.getInstance().isIntegratedServerRunning()) {
                surveyor$worldSummary = WorldSummary.of(SurveyorClient.stealServerWorld(self.getRegistryKey()));
            } else {
                surveyor$worldSummary = WorldSummary.load(self, SurveyorClient.getWorldSavePath(self), true);
            }
        }
        return surveyor$worldSummary;
    }
}
