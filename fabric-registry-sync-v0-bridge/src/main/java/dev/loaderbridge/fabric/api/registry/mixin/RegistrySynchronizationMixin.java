package dev.loaderbridge.fabric.api.registry.mixin;

import com.mojang.serialization.DynamicOps;
import dev.loaderbridge.fabric.api.registry.DynamicRegistrySyncOptions;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistrySynchronization.class)
abstract class RegistrySynchronizationMixin {
    @Inject(method = "packRegistry", at = @At("HEAD"), cancellable = true)
    private static <T> void loaderbridge$skipEmptyRegistry(
            DynamicOps<Tag> ops,
            RegistryDataLoader.RegistryData<T> data,
            RegistryAccess registryAccess,
            Set<KnownPack> knownPacks,
            BiConsumer<ResourceKey<? extends Registry<?>>,
                    List<RegistrySynchronization.PackedRegistryEntry>> output,
            CallbackInfo callback) {
        if (DynamicRegistrySyncOptions.shouldSkipEmpty(data.key(), registryAccess)) {
            System.out.println("LOADERBRIDGE_FABRIC_DYNAMIC_EMPTY_SKIPPED:"
                    + data.key().location());
            callback.cancel();
        }
    }

    @Inject(method = "networkedRegistries", at = @At("RETURN"), cancellable = true)
    private static void loaderbridge$skipEmptyRegistryTags(
            CallbackInfoReturnable<Stream<RegistryAccess.RegistryEntry<?>>> callback) {
        callback.setReturnValue(callback.getReturnValue().filter(entry ->
                !DynamicRegistrySyncOptions.shouldSkipEmpty(entry.key(), entry.value())));
    }
}
