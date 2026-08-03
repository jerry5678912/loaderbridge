package net.fabricmc.fabric.api.resource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public interface SimpleResourceReloadListener<T> extends IdentifiableResourceReloadListener {
    @Override
    default CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier,
            ResourceManager manager, ProfilerFiller loadProfiler, ProfilerFiller applyProfiler,
            Executor loadExecutor, Executor applyExecutor) {
        return load(manager, loadProfiler, loadExecutor).thenCompose(barrier::wait)
                .thenCompose(data -> apply(data, manager, applyProfiler, applyExecutor));
    }

    CompletableFuture<T> load(ResourceManager manager, ProfilerFiller profiler, Executor executor);

    CompletableFuture<Void> apply(T data, ResourceManager manager, ProfilerFiller profiler,
            Executor executor);
}
