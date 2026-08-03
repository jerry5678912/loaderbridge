package dev.loaderbridge.fabric.api.registry.mixin;

import dev.loaderbridge.fabric.api.registry.DynamicRegistryViewBridge;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryDataLoader.class)
abstract class RegistryDataLoaderSetupMixin {
    @Unique
    private static final ThreadLocal<Boolean> LOADERBRIDGE_SERVER_LOAD =
            ThreadLocal.withInitial(() -> false);

    @Inject(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;"
            + "Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)"
            + "Lnet/minecraft/core/RegistryAccess$Frozen;", at = @At("HEAD"))
    private static void loaderbridge$markServerLoad(CallbackInfoReturnable<?> callback) {
        LOADERBRIDGE_SERVER_LOAD.set(true);
    }

    @Redirect(method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;"
            + "Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)"
            + "Lnet/minecraft/core/RegistryAccess$Frozen;", at = @At(value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", ordinal = 0))
    private static <T> void loaderbridge$beforeServerRegistryLoad(
            List<T> loaders, Consumer<T> consumer) {
        if (LOADERBRIDGE_SERVER_LOAD.get()) {
            LOADERBRIDGE_SERVER_LOAD.set(false);
            Map<ResourceKey<? extends Registry<?>>, Registry<?>> registries =
                    new IdentityHashMap<>(loaders.size());
            for (T loader : loaders) {
                Registry<?> registry = ((RegistryDataLoaderLoaderAccessor) loader)
                        .loaderbridge$registry();
                registries.put(registry.key(), registry);
            }
            DynamicRegistrySetupCallback.EVENT.invoker()
                    .onRegistrySetup(new DynamicRegistryViewBridge(registries));
        }
        loaders.forEach(consumer);
    }
}
