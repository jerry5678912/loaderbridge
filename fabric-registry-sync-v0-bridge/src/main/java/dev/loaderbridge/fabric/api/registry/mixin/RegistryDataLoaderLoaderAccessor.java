package dev.loaderbridge.fabric.api.registry.mixin;

import net.minecraft.core.WritableRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.resources.RegistryDataLoader$Loader")
public interface RegistryDataLoaderLoaderAccessor {
    @Accessor("registry")
    WritableRegistry<?> loaderbridge$registry();
}
