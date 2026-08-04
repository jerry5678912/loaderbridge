package dev.loaderbridge.fabric.api.resource.conditions.mixin;

import dev.loaderbridge.fabric.api.resource.conditions.BridgeResourceConditions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadableServerResources.class)
public abstract class ReloadableServerResourcesMixin {
    @Inject(method = "loadResources", at = @At("HEAD"))
    private static void loaderbridge$captureEnabledFeatures(
            ResourceManager resourceManager,
            LayeredRegistryAccess<RegistryLayer> registries,
            FeatureFlagSet enabledFeatures,
            Commands.CommandSelection commandSelection,
            int functionPermissionLevel,
            Executor preparationExecutor,
            Executor applicationExecutor,
            CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> callback) {
        BridgeResourceConditions.beginServerResourceReload(enabledFeatures);
    }
}
