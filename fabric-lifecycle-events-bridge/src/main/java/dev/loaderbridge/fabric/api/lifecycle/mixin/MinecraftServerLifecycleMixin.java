package dev.loaderbridge.fabric.api.lifecycle.mixin;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies Fabric's exact save and datapack-reload hook points on Minecraft 1.21.1. */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerLifecycleMixin {
    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void loaderbridge$beforeReload(Collection<String> packs,
            CallbackInfoReturnable<CompletableFuture<Void>> callback) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.invoker().startDataPackReload(
                server, server.getServerResources().resourceManager());
    }

    @Inject(method = "reloadResources", at = @At("RETURN"))
    private void loaderbridge$afterReload(Collection<String> packs,
            CallbackInfoReturnable<CompletableFuture<Void>> callback) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        callback.getReturnValue().handleAsync((unused, failure) -> {
            CloseableResourceManager resources = server.getServerResources().resourceManager();
            ServerLifecycleEvents.END_DATA_PACK_RELOAD.invoker().endDataPackReload(
                    server, resources, failure == null);
            return unused;
        }, server);
    }

    @Inject(method = "saveAllChunks", at = @At("HEAD"))
    private void loaderbridge$beforeSave(boolean suppressLogs, boolean flush, boolean force,
            CallbackInfoReturnable<Boolean> callback) {
        ServerLifecycleEvents.BEFORE_SAVE.invoker().onBeforeSave(
                (MinecraftServer) (Object) this, flush, force);
    }

    @Inject(method = "saveAllChunks", at = @At("RETURN"))
    private void loaderbridge$afterSave(boolean suppressLogs, boolean flush, boolean force,
            CallbackInfoReturnable<Boolean> callback) {
        ServerLifecycleEvents.AFTER_SAVE.invoker().onAfterSave(
                (MinecraftServer) (Object) this, flush, force);
    }
}
