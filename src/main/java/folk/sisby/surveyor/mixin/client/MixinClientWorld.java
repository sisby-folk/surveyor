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
        if (surveyor$worldSummary == null) {
            if (MinecraftClient.getInstance().isIntegratedServerRunning()) {
                surveyor$worldSummary = ((SurveyorWorld) MinecraftClient.getInstance().getServer().getWorld(((ClientWorld) (Object) this).getRegistryKey())).surveyor$getWorldSummary();
            } else {
                surveyor$worldSummary = WorldSummary.load((ClientWorld) (Object) this, SurveyorClient.getWorldSavePath((ClientWorld) (Object) this), true);
            }
        }
        return surveyor$worldSummary;
    }
}
