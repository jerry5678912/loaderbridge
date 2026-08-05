package dev.loaderbridge.fabric.api.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;

class FabricResourceLoaderContractTest {
    @TempDir Path temporaryDirectory;

    @Test
    void providerPinsExactContractAndBaseDependency() {
        var descriptor = new FabricResourceLoaderBridgeProvider().descriptor();
        assertThat(descriptor.contractVersion()).isEqualTo("fabric-resource-loader-v0:1.3.1");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("1.3.1+5b5275af19-loaderbridge.2");
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

    @Test
    void registersBuiltinDataPackWhenTheModContainsItsNamespace() throws Exception {
        Path pack = temporaryDirectory.resolve("resourcepacks/fixture");
        Files.createDirectories(pack.resolve("data/loaderbridge_test/tags/item"));
        Files.writeString(pack.resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":48,\"description\":\"fixture\"}}");

        assertThat(ResourceManagerHelper.registerBuiltinResourcePack(
                ResourceLocation.fromNamespaceAndPath("loaderbridge_test", "fixture"),
                container(temporaryDirectory), ResourcePackActivationType.DEFAULT_ENABLED))
                .isTrue();
    }

    @Test
    void rejectsBuiltinPackWhoseDirectoryDoesNotExist() {
        assertThat(ResourceManagerHelper.registerBuiltinResourcePack(
                ResourceLocation.fromNamespaceAndPath("loaderbridge_test", "missing"),
                container(temporaryDirectory), ResourcePackActivationType.NORMAL))
                .isFalse();
    }

    @Test
    void exposesResourcesFromTheJarFileSystemUsedByTranslatedMods() throws Exception {
        Path jar = temporaryDirectory.resolve("translated-fixture.jar");
        try (var fileSystem = FileSystems.newFileSystem(
                URI.create("jar:" + jar.toUri()), Map.of("create", "true"))) {
            Path root = fileSystem.getPath("/");
            Path namespace = Files.createDirectories(root.resolve(
                    "resourcepacks/jar_pack/data/loaderbridge_jar"));
            Files.writeString(namespace.resolve("marker.txt"), "jar marker");
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    "loaderbridge_test", "jar_pack");
            assertThat(ResourceManagerHelper.registerBuiltinResourcePack(id,
                    container(root), ResourcePackActivationType.NORMAL)).isTrue();
            var definition = ResourceManagerHelperImpl.builtinPacks(PackType.SERVER_DATA).stream()
                    .filter(candidate -> candidate.id().equals(id))
                    .findFirst().orElseThrow();
            var location = ResourceManagerHelperImpl.packLocation(definition);

            try (var resources = ResourceManagerHelperImpl.openBuiltinResources(
                    definition, PackType.SERVER_DATA, location)) {
                assertThat(resources.getResource(PackType.SERVER_DATA,
                        ResourceLocation.fromNamespaceAndPath(
                                "loaderbridge_jar", "marker.txt"))).isNotNull();
            }
        }
    }

    @Test
    void exposesDefaultEnabledBuiltinPackThroughTheNativeRepositoryContract() throws Exception {
        Path packRoot = temporaryDirectory.resolve("resourcepacks/native_pack");
        Files.createDirectories(packRoot.resolve("data/loaderbridge_native/tags/item"));
        Files.writeString(packRoot.resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":48,\"description\":\"native fixture\"}}");
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "native_pack");
        assertThat(ResourceManagerHelper.registerBuiltinResourcePack(id,
                container(temporaryDirectory), ResourcePackActivationType.DEFAULT_ENABLED)).isTrue();

        var definition = ResourceManagerHelperImpl.builtinPacks(PackType.SERVER_DATA).stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst().orElseThrow();
        var location = ResourceManagerHelperImpl.packLocation(definition);
        var selection = ResourceManagerHelperImpl.packSelection(definition);

        assertThat(definition.displayName().getString()).isEqualTo("loaderbridge_test/native_pack");
        assertThat(location.source().decorate(location.title()).toString())
                .contains("Fixture Mod");
        assertThat(selection.required()).isFalse();
        assertThat(location.source().shouldAddAutomatically()).isTrue();
        assertThat(selection.defaultPosition()).isEqualTo(Pack.Position.TOP);
        try (var resources = ResourceManagerHelperImpl.openBuiltinResources(
                definition, PackType.SERVER_DATA, location)) {
            assertThat(resources.getNamespaces(PackType.SERVER_DATA))
                    .contains("loaderbridge_native");
        }
    }

    @Test
    void rejectsBuiltinPackRootThatEscapesThroughSymbolicLink() throws Exception {
        Path containerRoot = Files.createDirectories(temporaryDirectory.resolve("container"));
        Path outsidePack = temporaryDirectory.resolve("outside-pack");
        Files.createDirectories(outsidePack.resolve("data/loaderbridge_escape"));
        Path resourcePacks = Files.createDirectories(containerRoot.resolve("resourcepacks"));
        createSymbolicLinkOrSkip(resourcePacks.resolve("escape"), outsidePack);

        assertThat(ResourceManagerHelper.registerBuiltinResourcePack(
                ResourceLocation.fromNamespaceAndPath("loaderbridge_test", "escape"),
                container(containerRoot), ResourcePackActivationType.NORMAL)).isFalse();
    }

    @Test
    void doesNotExposeResourceFileThatEscapesThroughSymbolicLink() throws Exception {
        Path containerRoot = Files.createDirectories(temporaryDirectory.resolve("file-container"));
        Path packRoot = containerRoot.resolve("resourcepacks/file_escape");
        Path namespaceRoot = Files.createDirectories(
                packRoot.resolve("data/loaderbridge_escape"));
        Path outsideFile = temporaryDirectory.resolve("outside.txt");
        Files.writeString(outsideFile, "must remain inaccessible");
        createSymbolicLinkOrSkip(namespaceRoot.resolve("escaped.txt"), outsideFile);
        Files.writeString(namespaceRoot.resolve("inside.txt"), "inside");
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "file_escape");
        assertThat(ResourceManagerHelper.registerBuiltinResourcePack(id,
                container(containerRoot), ResourcePackActivationType.NORMAL)).isTrue();
        var definition = ResourceManagerHelperImpl.builtinPacks(PackType.SERVER_DATA).stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst().orElseThrow();
        var location = ResourceManagerHelperImpl.packLocation(definition);

        try (var resources = ResourceManagerHelperImpl.openBuiltinResources(
                definition, PackType.SERVER_DATA, location)) {
            assertThat(resources.getResource(PackType.SERVER_DATA,
                    ResourceLocation.fromNamespaceAndPath("loaderbridge_escape", "inside.txt")))
                    .isNotNull();
            assertThat(resources.getResource(PackType.SERVER_DATA,
                    ResourceLocation.fromNamespaceAndPath("loaderbridge_escape", "escaped.txt")))
                    .isNull();
        }
    }

    @Test
    void doesNotExposeOverlayThatEscapesThePackRoot() throws Exception {
        Path containerRoot = Files.createDirectories(temporaryDirectory.resolve("overlay-container"));
        Path packRoot = containerRoot.resolve("resourcepacks/overlay_escape");
        Files.createDirectories(packRoot.resolve("data/loaderbridge_overlay"));
        Path outsideOverlay = Files.createDirectories(containerRoot.resolve("outside-overlay/data/loaderbridge_overlay"));
        Files.writeString(outsideOverlay.resolve("escaped.txt"), "must remain inaccessible");
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "overlay_escape");
        assertThat(ResourceManagerHelper.registerBuiltinResourcePack(id,
                container(containerRoot), ResourcePackActivationType.NORMAL)).isTrue();
        var definition = ResourceManagerHelperImpl.builtinPacks(PackType.SERVER_DATA).stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst().orElseThrow();
        var location = ResourceManagerHelperImpl.packLocation(definition);

        try (var resources = ResourceManagerHelperImpl.openBuiltinResources(
                definition, PackType.SERVER_DATA, location);
                var overlay = ((FabricBuiltinPackResources) resources)
                        .createOverlay("../../outside-overlay")) {
            assertThat(overlay.getResource(PackType.SERVER_DATA,
                    ResourceLocation.fromNamespaceAndPath(
                            "loaderbridge_overlay", "escaped.txt"))).isNull();
        }
    }

    @Test
    void mapsNormalAndAlwaysEnabledActivationOntoNativeSelection() throws Exception {
        Files.createDirectories(temporaryDirectory.resolve(
                "resourcepacks/normal/data/loaderbridge_normal"));
        Files.createDirectories(temporaryDirectory.resolve(
                "resourcepacks/always/data/loaderbridge_always"));
        ResourceLocation normalId = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "normal");
        ResourceLocation alwaysId = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "always");
        assertThat(ResourceManagerHelper.registerBuiltinResourcePack(normalId,
                container(temporaryDirectory), ResourcePackActivationType.NORMAL)).isTrue();
        assertThat(ResourceManagerHelper.registerBuiltinResourcePack(alwaysId,
                container(temporaryDirectory), ResourcePackActivationType.ALWAYS_ENABLED)).isTrue();

        var definitions = ResourceManagerHelperImpl.builtinPacks(PackType.SERVER_DATA);
        var normal = definitions.stream().filter(pack -> pack.id().equals(normalId))
                .findFirst().orElseThrow();
        var always = definitions.stream().filter(pack -> pack.id().equals(alwaysId))
                .findFirst().orElseThrow();

        assertThat(ResourceManagerHelperImpl.packLocation(normal).source().shouldAddAutomatically())
                .isFalse();
        assertThat(ResourceManagerHelperImpl.packSelection(normal).required()).isFalse();
        assertThat(ResourceManagerHelperImpl.packLocation(always).source().shouldAddAutomatically())
                .isTrue();
        assertThat(ResourceManagerHelperImpl.packSelection(always).required()).isTrue();
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

    private static ModContainer container(Path root) {
        return new ModContainer() {
            @Override public ModMetadata getMetadata() {
                return (ModMetadata) java.lang.reflect.Proxy.newProxyInstance(
                        ModMetadata.class.getClassLoader(), new Class<?>[] { ModMetadata.class },
                        (proxy, method, arguments) -> switch (method.getName()) {
                            case "getName" -> "Fixture Mod";
                            case "getId" -> "loaderbridge_fixture";
                            case "toString" -> "Fixture Mod metadata";
                            default -> throw new UnsupportedOperationException(method.getName());
                        });
            }
            @Override public List<Path> getRootPaths() { return List.of(root); }
            @Override public ModOrigin getOrigin() { throw new UnsupportedOperationException(); }
            @Override public Optional<ModContainer> getContainingMod() { return Optional.empty(); }
            @Override public Collection<ModContainer> getContainedMods() { return List.of(); }
            @SuppressWarnings("deprecation")
            @Override public Path getRootPath() { return root; }
            @SuppressWarnings("deprecation")
            @Override public Path getPath(String file) { return root.resolve(file); }
        };
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | java.io.IOException | SecurityException exception) {
            Assumptions.assumeTrue(false,
                    "symbolic links are unavailable in this test environment: " + exception);
        }
    }
}
