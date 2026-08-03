package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.EntrypointException;
import net.fabricmc.loader.api.ModContainer;
import dev.loaderbridge.fabric.metadata.FabricMetadataParser;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BridgeFabricLoaderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void exposesRegisteredModsAliasesEntrypointsAndDirectories() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.configure(EnvType.SERVER, Path.of("build/test-game"));
        loader.registerMod(BridgeModContainer.create("fixture", "1.0.0", "Fixture", List.of("alias"),
                Path.of("build/fixture")));
        Runnable entrypoint = () -> {};
        loader.registerEntrypoint("main", entrypoint);

        assertThat(FabricLoader.getInstance()).isSameAs(loader);
        assertThat(loader.isModLoaded("alias")).isTrue();
        assertThat(loader.getModContainer("fixture")).isPresent();
        assertThat(loader.getEntrypoints("main", Runnable.class)).containsExactly(entrypoint);
        assertThat(loader.getEnvironmentType()).isEqualTo(EnvType.SERVER);
        assertThat(loader.getConfigDir().getFileName()).isEqualTo(Path.of("config"));
    }

    @Test
    void exposesEntrypointContainersAndAggregatesInvocationFailures() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        ModContainer first = BridgeModContainer.create("first", "1", "First", List.of(), Path.of("first"));
        ModContainer second = BridgeModContainer.create("second", "1", "Second", List.of(), Path.of("second"));
        Runnable one = () -> { throw new IllegalStateException("one"); };
        Runnable two = () -> { throw new IllegalArgumentException("two"); };
        loader.registerEntrypoint("main", first, "example.One", one);
        loader.registerEntrypoint("main", second, "example.Two", two);

        assertThat(loader.getEntrypointContainers("main", Runnable.class))
                .extracting(container -> container.getProvider().getMetadata().getId())
                .containsExactly("first", "second");
        assertThat(loader.getEntrypointContainers("main", Runnable.class))
                .extracting(container -> container.getDefinition())
                .containsExactly("example.One", "example.Two");
        assertThatThrownBy(() -> loader.invokeEntrypoints("main", Runnable.class, Runnable::run))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("provided by 'first'")
                .satisfies(error -> assertThat(error.getSuppressed()).hasSize(1));
    }

    @Test
    void resolvesCustomEntrypointsLazilyAndCachesOneInstancePerRequestedType() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        ModContainer provider = BridgeModContainer.create(
                "provider", "1", "Provider", List.of(), Path.of("provider"));
        AtomicInteger creations = new AtomicInteger();
        Runnable value = () -> { };
        loader.registerEntrypointDefinition("fixture-api", provider, "example.Api::INSTANCE",
                type -> {
                    creations.incrementAndGet();
                    return type.cast(value);
                });

        var containers = loader.getEntrypointContainers("fixture-api", Runnable.class);

        assertThat(creations).hasValue(0);
        assertThat(containers).singleElement().satisfies(container -> {
            assertThat(container.getProvider()).isSameAs(provider);
            assertThat(container.getDefinition()).isEqualTo("example.Api::INSTANCE");
            assertThat(container.getEntrypoint()).isSameAs(value);
            assertThat(container.getEntrypoint()).isSameAs(value);
        });
        assertThat(creations).hasValue(1);
    }

    @Test
    void reportsFabricCompatibleEntrypointResolutionFailures() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        ModContainer provider = BridgeModContainer.create(
                "provider", "1", "Provider", List.of(), Path.of("provider"));
        loader.registerMod(provider);
        loader.registerEntrypoint("client", provider, "example.NotRunnable", new Object());

        var containers = loader.getEntrypointContainers("client", Runnable.class);
        assertThat(containers).hasSize(1);
        assertThatThrownBy(() -> containers.getFirst().getEntrypoint())
                .isInstanceOf(EntrypointException.class)
                .hasMessageContaining("provided by 'provider'")
                .satisfies(error -> assertThat(((EntrypointException) error).getKey())
                        .isEqualTo("client"));
    }

    @Test
    void matchesFabricEnvironmentsExactly() {
        assertThat(ModEnvironment.CLIENT.matches(EnvType.CLIENT)).isTrue();
        assertThat(ModEnvironment.CLIENT.matches(EnvType.SERVER)).isFalse();
        assertThat(ModEnvironment.SERVER.matches(EnvType.SERVER)).isTrue();
        assertThat(ModEnvironment.SERVER.matches(EnvType.CLIENT)).isFalse();
        assertThat(ModEnvironment.UNIVERSAL.matches(EnvType.CLIENT)).isTrue();
        assertThat(ModEnvironment.UNIVERSAL.matches(EnvType.SERVER)).isTrue();
    }

    @Test
    void objectShareValidatesKeysAndNotifiesDeferredAndImmediateConsumers() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        var share = loader.getObjectShare();
        List<String> notifications = new ArrayList<>();

        share.whenAvailable("fixture:value", (key, value) -> notifications.add(key + "=" + value));
        assertThat(share.put("fixture:value", 42)).isNull();
        share.whenAvailable("fixture:value", (key, value) -> notifications.add("again=" + value));

        assertThat(notifications).containsExactly("fixture:value=42", "again=42");
        assertThatIllegalArgumentException().isThrownBy(() -> share.get("invalid"));
        assertThatNullPointerException().isThrownBy(() -> share.put("fixture:null", null));
    }

    @Test
    @SuppressWarnings("deprecation")
    void exposesRuntimeContextAndSanitizesLaunchArguments() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        Object game = new Object();
        loader.configure(EnvType.CLIENT, Path.of("build/test-game"), "1.21.1", true, game,
                new String[] {"--username", "Jerry", "--demo", "--accessToken", "secret"});

        assertThat(loader.getRawGameVersion()).isEqualTo("1.21.1");
        assertThat(loader.isDevelopmentEnvironment()).isTrue();
        assertThat(loader.getGameInstance()).isSameAs(game);
        assertThat(loader.getGameDirectory()).isEqualTo(loader.getGameDir().toFile());
        assertThat(loader.getConfigDirectory()).isEqualTo(loader.getConfigDir().toFile());
        assertThat(loader.getLaunchArguments(false)).containsExactly(
                "--username", "Jerry", "--demo", "--accessToken", "secret");
        assertThat(loader.getLaunchArguments(true)).containsExactly("--demo");
    }

    @Test
    @SuppressWarnings("deprecation")
    void hostReconfigurationPreservesCapturedArgumentsAndPublishedGameInstance() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        Object game = new Object();
        BridgeFabricLoader.captureLaunchArguments(
                new String[] {"--demo", "--accessToken", "secret"});
        loader.publishGameInstance(game);

        loader.configureHost(EnvType.CLIENT, Path.of("build/host-game"), "1.21.1", false);

        assertThat(loader.getLaunchArguments(false))
                .containsExactly("--demo", "--accessToken", "secret");
        assertThat(loader.getLaunchArguments(true)).containsExactly("--demo");
        assertThat(loader.getGameInstance()).isSameAs(game);
    }

    @Test
    void registersFabricCompatibleBuiltinAndLoaderContainersIdempotently() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        loader.configure(EnvType.CLIENT, Path.of("build/test-game"), "1.21.1", false,
                null, new String[0]);
        loader.configure(EnvType.CLIENT, Path.of("build/test-game"), "1.21.1", false,
                null, new String[0]);

        assertThat(loader.getAllMods())
                .extracting(container -> container.getMetadata().getId())
                .startsWith("fabricloader", "java", "minecraft")
                .doesNotHaveDuplicates();
        assertThat(loader.getModContainer("minecraft")).get()
                .satisfies(container -> {
                    assertThat(container.getMetadata().getType()).isEqualTo("builtin");
                    assertThat(container.getMetadata().getVersion().getFriendlyString())
                            .isEqualTo("1.21.1");
                    assertThat(container.getMetadata().getDepends()).singleElement()
                            .satisfies(dependency -> {
                                assertThat(dependency.getModId()).isEqualTo("java");
                                assertThat(dependency.matches(loader.getModContainer("java")
                                        .orElseThrow().getMetadata().getVersion())).isTrue();
                            });
                });
        assertThat(loader.getModContainer("java")).get()
                .satisfies(container -> {
                    assertThat(container.getMetadata().getType()).isEqualTo("builtin");
                    assertThat(container.getMetadata().getVersion().getFriendlyString())
                            .isEqualTo(System.getProperty("java.specification.version")
                                    .replaceFirst("^1\\.", ""));
                });
        assertThat(loader.getModContainer("fabricloader")).get()
                .satisfies(container -> {
                    assertThat(container.getMetadata().getType()).isEqualTo("fabric");
                    assertThat(container.getMetadata().getVersion().getFriendlyString())
                            .isEqualTo("0.16.14");
                });
    }

    @Test
    void exposesParsedAliasesEnvironmentAndDependencyKindsAtRuntime() throws Exception {
        var parsed = new FabricMetadataParser().parse("""
                {
                  "schemaVersion": 1,
                  "id": "rich_fixture",
                  "version": "2.0.0",
                  "name": "Rich Fixture",
                  "description": "Runtime metadata",
                  "authors": [{"name":"Jerry","contact":{"email":"jerry@example.invalid"}}],
                  "contributors": ["Helper"],
                  "contact": {"homepage":"https://example.invalid"},
                  "license": ["Apache-2.0"],
                  "icon": {"32":"small.png","128":"large.png"},
                  "custom": {"enabled":true,"settings":{"mode":"test"},"values":[1,"two"]},
                  "environment": "client",
                  "provides": ["rich_alias"],
                  "depends": {"minecraft": ">=1.21.1"},
                  "breaks": {"broken_mod": "*"}
                }
                """.getBytes(StandardCharsets.UTF_8));
        ModContainer container = BridgeModContainer.create(parsed, Path.of("build/rich-fixture"));

        assertThat(container.getMetadata().getEnvironment()).isEqualTo(ModEnvironment.CLIENT);
        assertThat(container.getMetadata().getDescription()).isEqualTo("Runtime metadata");
        assertThat(container.getMetadata().getAuthors()).singleElement()
                .satisfies(person -> {
                    assertThat(person.getName()).isEqualTo("Jerry");
                    assertThat(person.getContact().get("email")).contains("jerry@example.invalid");
                });
        assertThat(container.getMetadata().getContributors())
                .extracting(net.fabricmc.loader.api.metadata.Person::getName).containsExactly("Helper");
        assertThat(container.getMetadata().getContact().get("homepage"))
                .contains("https://example.invalid");
        assertThat(container.getMetadata().getLicense()).containsExactly("Apache-2.0");
        assertThat(container.getMetadata().getIconPath(64)).contains("large.png");
        assertThat(container.getMetadata().getIconPath(256)).contains("large.png");
        assertThat(container.getMetadata().getCustomValue("enabled").getAsBoolean()).isTrue();
        assertThat(container.getMetadata().getCustomValue("settings").getAsObject()
                .get("mode").getAsString()).isEqualTo("test");
        assertThat(container.getMetadata().getCustomValue("values").getAsArray().get(1)
                .getAsString()).isEqualTo("two");
        assertThat(container.getMetadata().getProvides()).containsExactly("rich_alias");
        assertThat(container.getMetadata().getDependencies())
                .extracting(ModDependency::getKind, ModDependency::getModId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(ModDependency.Kind.DEPENDS, "minecraft"),
                        org.assertj.core.groups.Tuple.tuple(ModDependency.Kind.BREAKS, "broken_mod"));
        assertThat(container.getMetadata().getDepends().iterator().next().matches(Version.parse("1.21.1")))
                .isTrue();
        ModDependency minecraft = container.getMetadata().getDepends().iterator().next();
        assertThat(minecraft.getVersionRequirements()).hasSize(1);
        assertThat(minecraft.getVersionIntervals()).singleElement()
                .satisfies(interval -> {
                    assertThat(interval.getMin().getFriendlyString()).isEqualTo("1.21.1");
                    assertThat(interval.getMax()).isNull();
                });
    }

    @Test
    void resolvesNestedContainmentRegardlessOfRegistrationOrder() throws Exception {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        FabricMetadataParser parser = new FabricMetadataParser();
        var childMetadata = parser.parse("""
                {"schemaVersion":1,"id":"child","version":"1"}
                """.getBytes(StandardCharsets.UTF_8));
        var parentMetadata = parser.parse("""
                {"schemaVersion":1,"id":"parent","version":"1"}
                """.getBytes(StandardCharsets.UTF_8));
        BridgeModContainer child = BridgeModContainer.create(
                childMetadata, Path.of("child"), "parent", "META-INF/jars/child.jar");
        BridgeModContainer parent = BridgeModContainer.create(parentMetadata, Path.of("parent"));

        loader.registerMod(child);
        assertThat(child.getContainingMod()).isEmpty();
        loader.registerMod(parent);

        assertThat(child.getContainingMod()).contains(parent);
        assertThat(parent.getContainedMods()).containsExactly(child);
        assertThat(child.getOrigin().getKind())
                .isEqualTo(net.fabricmc.loader.api.metadata.ModOrigin.Kind.NESTED);
        assertThat(child.getOrigin().getParentModId()).isEqualTo("parent");
        assertThat(child.getOrigin().getParentSubLocation()).isEqualTo("META-INF/jars/child.jar");
    }

    @Test
    void resolvesClasspathResourcesAcrossMultipleRootsInOrder() throws Exception {
        Path first = Files.createDirectories(temporaryDirectory.resolve("first"));
        Path second = Files.createDirectories(temporaryDirectory.resolve("second/assets/fixture"));
        Path resource = Files.writeString(second.resolve("value.txt"), "found");
        ModContainer container = new BridgeModContainer(
                BridgeModContainer.create("classpath", "1", "Classpath", List.of(), first).metadata(),
                List.of(first, temporaryDirectory.resolve("second")), null, null);

        assertThat(container.getRootPaths()).containsExactly(first, temporaryDirectory.resolve("second"));
        assertThat(container.findPath("assets/fixture/value.txt")).contains(resource);
        assertThat(container.findPath("assets/fixture/missing.txt")).isEmpty();
    }
}
