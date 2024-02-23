package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements SurveyorWorld {
    @Unique WorldSummary surveyor$worldSummary = null;

    @Override
    public WorldSummary surveyor$getWorldSummary() {
        return surveyor$worldSummary;
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/DimensionOptions;chunkGenerator()Lnet/minecraft/world/gen/chunk/ChunkGenerator;"))
    public void loadSummary(CallbackInfo ci) {
        surveyor$worldSummary = WorldSummary.load((ServerWorld) (Object) this);
    }

    @Inject(method = "saveLevel", at = @At("TAIL"))
    public void saveSummary(CallbackInfo ci) {
        if (surveyor$worldSummary != null) surveyor$worldSummary.save((ServerWorld) (Object) this);
    }
}
