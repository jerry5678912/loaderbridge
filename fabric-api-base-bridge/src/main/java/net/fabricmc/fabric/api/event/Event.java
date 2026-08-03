package net.fabricmc.fabric.api.event;

import net.minecraft.resources.ResourceLocation;

/** Binary-compatible public Fabric event contract for the LoaderBridge runtime. */
public abstract class Event<T> {
    public static final ResourceLocation DEFAULT_PHASE =
            ResourceLocation.fromNamespaceAndPath("fabric", "default");

    protected volatile T invoker;

    public final T invoker() {
        return invoker;
    }

    public abstract void register(T listener);

    public void register(ResourceLocation phase, T listener) {
        register(listener);
    }

    public void addPhaseOrdering(ResourceLocation firstPhase, ResourceLocation secondPhase) {
        // Retained for compatibility with third-party Event subclasses.
    }
}
