package dev.loaderbridge.fabric.api.registry;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback;
import net.minecraft.resources.ResourceLocation;

/** Immutable Fabric remap view translated from one Forge ID-mapping event. */
public record RegistryRemapState<T>(
        Int2IntMap rawIdChangeMap,
        Int2ObjectMap<ResourceLocation> oldIds,
        Int2ObjectMap<ResourceLocation> newIds)
        implements RegistryIdRemapCallback.RemapState<T> {

    public RegistryRemapState {
        rawIdChangeMap = Int2IntMaps.unmodifiable(new Int2IntOpenHashMap(rawIdChangeMap));
        oldIds = Int2ObjectMaps.unmodifiable(new Int2ObjectOpenHashMap<>(oldIds));
        newIds = Int2ObjectMaps.unmodifiable(new Int2ObjectOpenHashMap<>(newIds));
    }

    @Override
    public Int2IntMap getRawIdChangeMap() {
        return rawIdChangeMap;
    }

    @Override
    public ResourceLocation getIdFromOld(int oldRawId) {
        return oldIds.get(oldRawId);
    }

    @Override
    public ResourceLocation getIdFromNew(int newRawId) {
        return newIds.get(newRawId);
    }
}
