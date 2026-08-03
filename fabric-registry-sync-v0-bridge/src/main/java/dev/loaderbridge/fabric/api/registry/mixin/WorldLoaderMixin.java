package dev.loaderbridge.fabric.api.registry.mixin;

import dev.loaderbridge.fabric.api.registry.DynamicRegistryRuntime;
import java.util.List;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.WorldLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WorldLoader.class)
abstract class WorldLoaderMixin {
    @ModifyArg(method = "load", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/WorldLoader;loadAndReplaceLayer("
                    + "Lnet/minecraft/server/packs/resources/ResourceManager;"
                    + "Lnet/minecraft/core/LayeredRegistryAccess;"
                    + "Lnet/minecraft/server/RegistryLayer;Ljava/util/List;)"
                    + "Lnet/minecraft/core/LayeredRegistryAccess;"), index = 3)
    private static List<RegistryDataLoader.RegistryData<?>> loaderbridge$loadDynamicRegistries(
            List<RegistryDataLoader.RegistryData<?>> vanilla) {
        return DynamicRegistryRuntime.descriptors();
    }
}
