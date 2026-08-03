package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.RegistryAccess;

/** Fabric lifecycle API 2.6.0 common tag-loading contract. */
public final class CommonLifecycleEvents {
    public static final Event<TagsLoaded> TAGS_LOADED = EventFactory.createArrayBacked(
            TagsLoaded.class, callbacks -> (registries, client) -> {
                for (TagsLoaded callback : callbacks) {
                    callback.onTagsLoaded(registries, client);
                }
            });

    private CommonLifecycleEvents() {}

    @FunctionalInterface
    public interface TagsLoaded {
        void onTagsLoaded(RegistryAccess registries, boolean client);
    }
}
