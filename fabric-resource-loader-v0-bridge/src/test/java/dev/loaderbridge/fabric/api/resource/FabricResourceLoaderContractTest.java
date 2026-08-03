package dev.loaderbridge.fabric.api.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.junit.jupiter.api.Test;

class FabricResourceLoaderContractTest {
    @Test
    void providerPinsExactContractAndBaseDependency() {
        var descriptor = new FabricResourceLoaderBridgeProvider().descriptor();
        assertThat(descriptor.contractVersion()).isEqualTo("fabric-resource-loader-v0:1.3.1");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("1.3.1+5b5275af19-loaderbridge.1");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-resource-loader-v0", "1.3.1+5b5275af19");
        assertThat(descriptor.requiredModules()).containsExactly("fabric-api-base-bridge");
        assertThat(descriptor.providedClasses()).contains(
                "net.fabricmc.fabric.api.resource.ResourceManagerHelper",
                "net.fabricmc.fabric.api.resource.SimpleResourceReloadListener");
    }

    @Test
    void activationTypesMatchFabricDefaultRules() {
        assertThat(ResourcePackActivationType.NORMAL.isEnabledByDefault()).isFalse();
        assertThat(ResourcePackActivationType.DEFAULT_ENABLED.isEnabledByDefault()).isTrue();
        assertThat(ResourcePackActivationType.ALWAYS_ENABLED.isEnabledByDefault()).isTrue();
    }

    @Test
    void serverListenersRespectDeclaredFabricDependencies() {
        ResourceManagerHelperImpl helper = ResourceManagerHelperImpl.get(PackType.SERVER_DATA);
        ResourceLocation first = ResourceLocation.fromNamespaceAndPath("loaderbridge_test", "first");
        ResourceLocation second = ResourceLocation.fromNamespaceAndPath("loaderbridge_test", "second");
        helper.registerReloadListener(listener(second, List.of(first)));
        helper.registerReloadListener(listener(first, List.of(ResourceReloadListenerKeys.TAGS)));

        assertThat(helper.listeners(null)).extracting(IdentifiableResourceReloadListener::getFabricId)
                .containsSubsequence(first, second);
    }

    @Test
    void unresolvedDependenciesProduceStableFailureInsteadOfWrongOrdering() {
        ResourceManagerHelperImpl helper = ResourceManagerHelperImpl.get(PackType.CLIENT_RESOURCES);
        ResourceLocation listener = ResourceLocation.fromNamespaceAndPath("loaderbridge_test", "blocked");
        ResourceLocation missing = ResourceLocation.fromNamespaceAndPath("loaderbridge_test", "missing");
        helper.registerReloadListener(listener(listener, List.of(missing)));

        assertThatThrownBy(() -> helper.listeners(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LB-FAPI-RESOURCE-001");
    }

    private static IdentifiableResourceReloadListener listener(ResourceLocation id,
            List<ResourceLocation> dependencies) {
        return new IdentifiableResourceReloadListener() {
            @Override public ResourceLocation getFabricId() { return id; }
            @Override public List<ResourceLocation> getFabricDependencies() { return dependencies; }
            @Override public java.util.concurrent.CompletableFuture<Void> reload(
                    PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager,
                    ProfilerFiller loadProfiler, ProfilerFiller applyProfiler,
                    java.util.concurrent.Executor loadExecutor,
                    java.util.concurrent.Executor applyExecutor) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
        };
    }
}
