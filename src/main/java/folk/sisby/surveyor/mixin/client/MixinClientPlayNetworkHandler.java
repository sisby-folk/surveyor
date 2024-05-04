package folk.sisby.surveyor.mixin.client;

import com.mojang.authlib.GameProfile;
import folk.sisby.surveyor.client.NetworkHandlerSummary;
import folk.sisby.surveyor.client.SurveyorNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler implements SurveyorNetworkHandler {
    @Unique NetworkHandlerSummary surveyor$summary = null;

    @Override
    public NetworkHandlerSummary surveyor$getSummary() {
        return surveyor$summary;
    }

    @Accessor public abstract GameProfile getProfile();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(MinecraftClient client, ClientConnection clientConnection, ClientConnectionState clientConnectionState, CallbackInfo ci) {
        ClientPlayNetworkHandler self = (ClientPlayNetworkHandler) (Object) this;
        surveyor$summary = new NetworkHandlerSummary(self);
    }
}
