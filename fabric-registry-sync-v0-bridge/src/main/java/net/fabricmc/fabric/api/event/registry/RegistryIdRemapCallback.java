package net.fabricmc.fabric.api.event.registry;

import dev.loaderbridge.fabric.api.registry.RegistryEventDispatcher;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
public interface RegistryIdRemapCallback<T> {
    void onRemap(RemapState<T> state);

    interface RemapState<T> {
        Int2IntMap getRawIdChangeMap();

        ResourceLocation getIdFromOld(int oldRawId);

        ResourceLocation getIdFromNew(int newRawId);
    }

    static <T> Event<RegistryIdRemapCallback<T>> event(Registry<T> registry) {
        return RegistryEventDispatcher.remapped(registry);
    }
}
