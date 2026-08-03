package dev.loaderbridge.fabric.api.registry;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.minecraft.resources.ResourceKey;

/** Process-wide attributes keyed by the registry's stable resource key. */
public final class RegistryAttributeStore {
    private static final Map<ResourceKey<?>, Holder> HOLDERS = new ConcurrentHashMap<>();

    private RegistryAttributeStore() {
    }

    public static RegistryAttributeHolder get(ResourceKey<?> key) {
        return HOLDERS.computeIfAbsent(key, ignored -> new Holder());
    }

    private static final class Holder implements RegistryAttributeHolder {
        private final EnumSet<RegistryAttribute> attributes = EnumSet.noneOf(RegistryAttribute.class);

        @Override
        public synchronized RegistryAttributeHolder addAttribute(RegistryAttribute attribute) {
            attributes.add(java.util.Objects.requireNonNull(attribute, "attribute"));
            return this;
        }

        @Override
        public synchronized boolean hasAttribute(RegistryAttribute attribute) {
            return attributes.contains(attribute);
        }
    }
}
