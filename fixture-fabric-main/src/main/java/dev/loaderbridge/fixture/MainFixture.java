package dev.loaderbridge.fixture;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class MainFixture implements ModInitializer {
    @Override
    @SuppressWarnings("deprecation")
    public void onInitialize() {
        var loader = FabricLoader.getInstance();
        var custom = loader.getEntrypointContainers(
                "loaderbridge:fixture_api", FixtureApi.class);
        if (custom.size() != 1
                || !custom.getFirst().getProvider().getMetadata().getId()
                        .equals("loaderbridge_fixture")
                || !custom.getFirst().getDefinition()
                        .equals("dev.loaderbridge.fixture.FixtureApiProvider")
                || custom.getFirst().getEntrypoint() != custom.getFirst().getEntrypoint()
                || !custom.getFirst().getEntrypoint().value().equals("custom-entrypoint")) {
            throw new IllegalStateException("custom Fabric entrypoint contract failed");
        }
        var constructor = loader.getEntrypoints(
                "loaderbridge:constructor", FixtureApiFactory.class);
        if (constructor.size() != 1
                || !constructor.getFirst().create().value().equals("custom-entrypoint")) {
            throw new IllegalStateException("constructor Fabric entrypoint contract failed");
        }
        System.out.println("LOADERBRIDGE_FIXTURE_CONSTRUCTOR_ENTRYPOINT_READY");
        var kotlinEntrypoints = loader.getEntrypoints(
                "loaderbridge:kotlin", Runnable.class);
        if (kotlinEntrypoints.size() != 8) {
            throw new IllegalStateException(
                    "unexpected Kotlin entrypoint count: " + kotlinEntrypoints.size());
        }
        kotlinEntrypoints.forEach(Runnable::run);
        System.out.println("LOADERBRIDGE_FIXTURE_KOTLIN_ADAPTER_READY");
        var nestedChild = loader.getModContainer("loaderbridge_nested_child").orElseThrow();
        var parent = loader.getModContainer("loaderbridge_fixture").orElseThrow();
        if (nestedChild.getContainingMod().orElseThrow() != parent
                || !parent.getContainedMods().contains(nestedChild)) {
            throw new IllegalStateException("nested Fabric child was not registered");
        }
        System.out.println("LOADERBRIDGE_FIXTURE_NESTED_CONTAINMENT_READY");
        if (!loader.getRawGameVersion().equals("1.21.1")) {
            throw new IllegalStateException(
                    "unexpected raw game version: " + loader.getRawGameVersion());
        }
        assertRuntimeContainer(loader, "minecraft", "builtin", "1.21.1");
        assertRuntimeContainer(loader, "java", "builtin",
                System.getProperty("java.specification.version").replaceFirst("^1\\.", ""));
        assertRuntimeContainer(loader, "fabricloader", "fabric", "0.16.14");
        var loaderContainer = loader.getModContainer("fabricloader").orElseThrow();
        if (!loaderContainer.getMetadata().getLicense().contains("Apache-2.0")
                || loaderContainer.findPath("assets/fabricloader/icon.png").isEmpty()) {
            throw new IllegalStateException("Fabric Loader self metadata or resources are unavailable");
        }
        System.out.println("LOADERBRIDGE_FIXTURE_LOADER_METADATA_READY");
        var minecraftRoots = loader.getModContainer("minecraft").orElseThrow().getRootPaths();
        if (minecraftRoots.isEmpty()
                || minecraftRoots.stream().anyMatch(loader.getGameDir()::equals)) {
            throw new IllegalStateException(
                    "Minecraft builtin roots do not describe Forge's game inputs: "
                            + minecraftRoots);
        }
        System.out.println("LOADERBRIDGE_FIXTURE_BUILTIN_ROOTS_READY");
        String[] arguments = loader.getLaunchArguments(false);
        boolean expectedArguments = loader.getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT
                ? java.util.List.of(arguments).contains("--gameDir")
                : java.util.List.of(arguments).contains("nogui");
        if (!expectedArguments) {
            throw new IllegalStateException(
                    "final Minecraft launch arguments were not captured: "
                            + java.util.Arrays.toString(arguments));
        }
        if (loader.getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            Object game = loader.getGameInstance();
            if (game == null || !game.getClass().getName().equals("net.minecraft.client.Minecraft")) {
                throw new IllegalStateException("Fabric client game instance is unavailable");
            }
            System.out.println("LOADERBRIDGE_FIXTURE_CLIENT_GAME_INSTANCE_READY");
        } else if (loader.getGameInstance() != null) {
            throw new IllegalStateException(
                    "Fabric server game instance must be null before server construction");
        } else {
            Thread.ofPlatform().daemon().name("loaderbridge-fixture-server-instance").start(() -> {
                long deadline = System.nanoTime() + java.time.Duration.ofSeconds(30).toNanos();
                while (System.nanoTime() < deadline) {
                    if (loader.getGameInstance() != null) {
                        System.out.println("LOADERBRIDGE_FIXTURE_SERVER_GAME_INSTANCE_READY");
                        return;
                    }
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                System.err.println("LOADERBRIDGE_FIXTURE_SERVER_GAME_INSTANCE_TIMEOUT");
            });
        }
        System.out.println("LOADERBRIDGE_FIXTURE_CUSTOM_ENTRYPOINT_READY");
        System.out.println("LOADERBRIDGE_FIXTURE_RAW_GAME_VERSION=1.21.1");
        System.out.println("LOADERBRIDGE_FIXTURE_BUILTIN_MODS_READY");
        System.out.println("LOADERBRIDGE_FIXTURE_LAUNCH_ARGUMENTS_READY");
        System.out.println("LOADERBRIDGE_FIXTURE_MAIN_READY");
    }

    private static void assertRuntimeContainer(FabricLoader loader, String id, String type,
            String version) {
        var container = loader.getModContainer(id).orElseThrow(
                () -> new IllegalStateException("missing Fabric runtime container: " + id));
        if (!loader.isModLoaded(id)
                || !container.getMetadata().getType().equals(type)
                || !container.getMetadata().getVersion().getFriendlyString().equals(version)) {
            throw new IllegalStateException("invalid Fabric runtime container: " + id);
        }
    }
}
