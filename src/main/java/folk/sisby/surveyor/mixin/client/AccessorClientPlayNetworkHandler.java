package folk.sisby.surveyor.mixin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayNetworkHandler.class)
public interface AccessorClientPlayNetworkHandler {
    @Accessor()
    GameProfile getProfile();
}
