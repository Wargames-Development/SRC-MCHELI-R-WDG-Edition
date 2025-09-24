package mcheli.mixin;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @Inject(
        method = "processUseEntity(Lnet/minecraft/network/play/client/C02PacketUseEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetHandlerPlayServer;kickPlayerFromServer(Ljava/lang/String;)V"
        ),
        cancellable = true)
    private void cancelInvalidEntityKick(C02PacketUseEntity packet, CallbackInfo ci) {
        System.out.println("123");
        ci.cancel();
    }
}
