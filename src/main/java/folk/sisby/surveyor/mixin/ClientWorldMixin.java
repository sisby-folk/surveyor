package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.chunk.ChunkSummaryState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientWorld.class)
public class ClientWorldMixin implements SurveyorWorld {
    @Unique ChunkSummaryState surveyor$chunkSummaryState = null;

    @Override
    public ChunkSummaryState surveyor$getChunkSummaryState() {
        if (surveyor$chunkSummaryState == null) {
            surveyor$chunkSummaryState = MinecraftClient.getInstance().isIntegratedServerRunning() ?
                ((SurveyorWorld) MinecraftClient.getInstance().getServer().getWorld(((ClientWorld) (Object) this).getRegistryKey())).surveyor$getChunkSummaryState() :
                ChunkSummaryState.load((ClientWorld) (Object) this);
        }
        return surveyor$chunkSummaryState;
    }
}
