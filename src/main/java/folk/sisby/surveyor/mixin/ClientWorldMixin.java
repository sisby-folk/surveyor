package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientWorld.class)
public class ClientWorldMixin implements SurveyorWorld {
    @Unique
    WorldSummary surveyor$worldSummary = null;

    @Override
    public WorldSummary surveyor$getWorldSummary() {
        if (surveyor$worldSummary == null) {
            surveyor$worldSummary = MinecraftClient.getInstance().isIntegratedServerRunning() ?
                ((SurveyorWorld) MinecraftClient.getInstance().getServer().getWorld(((ClientWorld) (Object) this).getRegistryKey())).surveyor$getWorldSummary() :
                WorldSummary.load((ClientWorld) (Object) this);
        }
        return surveyor$worldSummary;
    }
}
