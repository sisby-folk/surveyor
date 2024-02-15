package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.chunk.ChunkSummaryState;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements SurveyorWorld {
    @Unique ChunkSummaryState surveyor$chunkSummaryState = null;

    @Override
    public ChunkSummaryState surveyor$getChunkSummaryState() {
        if (surveyor$chunkSummaryState == null) surveyor$chunkSummaryState = ChunkSummaryState.load((ServerWorld) (Object) this);
        return surveyor$chunkSummaryState;
    }

    @Inject(method = "saveLevel", at = @At("TAIL"))
    public void saveSummaries(CallbackInfo ci) {
        if (surveyor$chunkSummaryState != null) surveyor$chunkSummaryState.save((ServerWorld) (Object) this);
    }
}
