package dev.loaderbridge.fabric.api.lifecycle;

import net.minecraft.server.level.FullChunkStatus;

/** Tracks the last chunk status exposed through the Fabric compatibility contract. */
public interface ChunkLifecycleTracker {
    FullChunkStatus loaderbridge$getCurrentEventStatus();

    void loaderbridge$setCurrentEventStatus(FullChunkStatus status);
}
