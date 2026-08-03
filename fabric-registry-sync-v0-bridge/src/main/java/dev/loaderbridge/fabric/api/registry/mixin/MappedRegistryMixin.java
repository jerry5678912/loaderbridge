package dev.loaderbridge.fabric.api.registry.mixin;

import dev.loaderbridge.fabric.api.registry.RegistryEventDispatcher;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
abstract class MappedRegistryMixin<T> {
    @Inject(method = "register", at = @At("RETURN"))
    private void loaderbridge$entryAdded(ResourceKey<T> key, T value,
            RegistrationInfo registrationInfo,
            CallbackInfoReturnable<Holder.Reference<T>> callback) {
        @SuppressWarnings("unchecked")
        MappedRegistry<T> registry = (MappedRegistry<T>) (Object) this;
        RegistryEventDispatcher.fireEntryAdded(registry, registry.getId(value), key.location(), value);
    }
}
