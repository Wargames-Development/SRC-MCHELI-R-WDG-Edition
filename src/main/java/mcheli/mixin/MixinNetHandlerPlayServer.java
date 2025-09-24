package mcheli.mixin;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetHandlerPlayServer.class, remap = false)
public abstract class MixinNetHandlerPlayServer {

    @Inject(method = "func_147340_a",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/NetHandlerPlayServer;kickPlayerFromServer(Ljava/lang/String;)V"),
        cancellable = true)
    private void cancelInvalidEntityKick(C02PacketUseEntity packet, CallbackInfo ci) {
        System.out.println("Mixin intercepts kick");
        ci.cancel();
    }

    @Inject(method = "func_147340_a", at = @At("HEAD"))
    private void onProcessUseEntity(C02PacketUseEntity packet, CallbackInfo ci) {
        System.out.println("[MixinTest] processUseEntity called");
    }
}
