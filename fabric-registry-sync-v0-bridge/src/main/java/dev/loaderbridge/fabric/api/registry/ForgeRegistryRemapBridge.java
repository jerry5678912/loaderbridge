package dev.loaderbridge.fabric.api.registry;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IdMappingEvent;

/** Converts Forge registry ID remaps into Fabric registry-local callbacks. */
final class ForgeRegistryRemapBridge {
    private ForgeRegistryRemapBridge() {
    }

    static void onIdMapping(IdMappingEvent event) {
        for (ResourceLocation registryId : event.getRegistries()) {
            Registry<?> registry = BuiltInRegistries.REGISTRY.get(registryId);
            if (registry == null) continue;

            Map<ResourceLocation, IdMappingEvent.ModRemapping> changes = new HashMap<>();
            for (IdMappingEvent.ModRemapping remapping : event.getRemaps(registryId)) {
                changes.put(remapping.key, remapping);
            }
            if (changes.isEmpty()) continue;
            fire(registry, changes);
        }
    }

    private static <T> void fire(Registry<T> registry,
            Map<ResourceLocation, IdMappingEvent.ModRemapping> changes) {
        var rawChanges = new Int2IntOpenHashMap();
        var oldIds = new Int2ObjectOpenHashMap<ResourceLocation>();
        var newIds = new Int2ObjectOpenHashMap<ResourceLocation>();
        for (T value : registry) {
            ResourceLocation id = registry.getKey(value);
            int newId = registry.getId(value);
            IdMappingEvent.ModRemapping change = changes.get(id);
            int oldId = change == null ? newId : change.oldId;
            rawChanges.put(oldId, newId);
            oldIds.put(oldId, id);
            newIds.put(newId, id);
        }
        RegistryEventDispatcher.fireRemapped(
                registry, new RegistryRemapState<>(rawChanges, oldIds, newIds));
    }
}
