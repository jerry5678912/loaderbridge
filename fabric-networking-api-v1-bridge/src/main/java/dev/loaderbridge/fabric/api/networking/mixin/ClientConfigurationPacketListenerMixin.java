package dev.loaderbridge.fabric.api.networking.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientConfigurationPacketListenerImpl.class, priority = 900)
public abstract class ClientConfigurationPacketListenerMixin {
    @Unique private boolean loaderbridge$configurationStarted;
    @Unique private boolean loaderbridge$configurationComplete;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void loaderbridge$init(CallbackInfo callback) {
        var handler = (ClientConfigurationPacketListenerImpl) (Object) this;
        ClientConfigurationNetworking.setCurrent(handler);
        ClientConfigurationConnectionEvents.INIT.invoker()
                .onConfigurationInit(handler, Minecraft.getInstance());
    }

    @Inject(method = "handleEnabledFeatures", at = @At("HEAD"))
    private void loaderbridge$start(CallbackInfo callback) {
        if (loaderbridge$configurationStarted) return;
        loaderbridge$configurationStarted = true;
        ClientConfigurationConnectionEvents.START.invoker().onConfigurationStart(
                (ClientConfigurationPacketListenerImpl) (Object) this, Minecraft.getInstance());
    }

    @Inject(method = "handleConfigurationFinished", at = @At("HEAD"))
    @SuppressWarnings("deprecation")
    private void loaderbridge$complete(CallbackInfo callback) {
        if (loaderbridge$configurationComplete) return;
        loaderbridge$configurationComplete = true;
        var handler = (ClientConfigurationPacketListenerImpl) (Object) this;
        ClientConfigurationConnectionEvents.COMPLETE.invoker()
                .onConfigurationComplete(handler, Minecraft.getInstance());
        ClientConfigurationConnectionEvents.READY.invoker()
                .onConfigurationReady(handler, Minecraft.getInstance());
        ClientConfigurationNetworking.clear(handler);
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void loaderbridge$disconnect(CallbackInfo callback) {
        if (loaderbridge$configurationComplete) return;
        loaderbridge$configurationComplete = true;
        var handler = (ClientConfigurationPacketListenerImpl) (Object) this;
        ClientConfigurationConnectionEvents.DISCONNECT.invoker()
                .onConfigurationDisconnect(handler, Minecraft.getInstance());
        ClientConfigurationNetworking.clear(handler);
    }
}
