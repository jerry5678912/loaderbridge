package dev.loaderbridge.fabric.api.networking.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerConfigurationPacketListenerImpl.class, priority = 900)
public abstract class ServerConfigurationPacketListenerMixin {
    @Unique private boolean loaderbridge$configurationStarted;
    @Unique private boolean loaderbridge$disconnected;

    @Inject(method = "startConfiguration", at = @At("HEAD"))
    private void loaderbridge$startConfiguration(CallbackInfo callback) {
        if (loaderbridge$configurationStarted) return;
        loaderbridge$configurationStarted = true;
        var handler = (ServerConfigurationPacketListenerImpl) (Object) this;
        MinecraftServer server = ServerConfigurationNetworking.getServer(handler);
        ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.invoker()
                .onSendConfiguration(handler, server);
        ServerConfigurationConnectionEvents.CONFIGURE.invoker()
                .onSendConfiguration(handler, server);
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void loaderbridge$disconnect(CallbackInfo callback) {
        if (loaderbridge$disconnected) return;
        loaderbridge$disconnected = true;
        var handler = (ServerConfigurationPacketListenerImpl) (Object) this;
        ServerConfigurationConnectionEvents.DISCONNECT.invoker()
                .onConfigureDisconnect(handler, ServerConfigurationNetworking.getServer(handler));
        ServerConfigurationNetworking.clear(handler);
    }
}
