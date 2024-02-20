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
    @Unique
    WorldSummary surveyor$worldSummary = null;

    @Override
    public WorldSummary surveyor$getWorldSummary() {
        if (surveyor$worldSummary == null) surveyor$worldSummary = WorldSummary.load((ServerWorld) (Object) this);
        return surveyor$worldSummary;
    }

    @Inject(method = "saveLevel", at = @At("TAIL"))
    public void saveSummaries(CallbackInfo ci) {
        if (surveyor$worldSummary != null) surveyor$worldSummary.save((ServerWorld) (Object) this);
    }
}
