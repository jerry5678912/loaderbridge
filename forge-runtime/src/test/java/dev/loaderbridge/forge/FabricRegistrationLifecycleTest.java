package dev.loaderbridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.fabric.runtime.BridgeFabricLoader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FabricRegistrationLifecycleTest {
    @Test
    @SuppressWarnings("deprecation")
    void publishesTheClientGameInstanceBeforeFabricInitialization() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.configure(net.fabricmc.api.EnvType.CLIENT, java.nio.file.Path.of("."),
                "1.21.1", false, null, new String[0]);

        FabricRegistrationLifecycle.publishClientGameInstance(getClass().getClassLoader());

        assertThat(loader.getGameInstance()).isSameAs(net.minecraft.client.Minecraft.getInstance());
    }

    @Test
    @SuppressWarnings("deprecation")
    void publishesTheDedicatedServerAfterItsConstructionEvent() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.configure(net.fabricmc.api.EnvType.SERVER, java.nio.file.Path.of("."),
                "1.21.1", false, null, new String[0]);
        Object server = new Object();

        FabricServerGameInstanceRegistration.publishServerInstance(new Object() {
            @SuppressWarnings("unused")
            public Object getServer() {
                return server;
            }
        });

        assertThat(loader.getGameInstance()).isSameAs(server);
    }

    @Test
    void invokesAllPreLaunchCallbacksAtTheFirstConstructEvent() {
        var coordinator = new FabricRegistrationLifecycle.PreLaunchCoordinator();
        List<String> order = new ArrayList<>();
        coordinator.register("zulu", () -> order.add("pre-zulu"));
        coordinator.register("alpha", () -> order.add("pre-alpha"));

        assertThat(coordinator.invokeIfConstructEvent(
                "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent")).isFalse();
        assertThat(coordinator.invokeIfConstructEvent(
                "net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent")).isTrue();
        assertThat(coordinator.invokeIfConstructEvent(
                "net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent")).isFalse();

        assertThat(order).containsExactly("pre-alpha", "pre-zulu");
    }

    @Test
    void invokesResolvedEntrypointsTogetherDuringCommonSetup() {
        var coordinator = new FabricRegistrationLifecycle.Coordinator();
        List<String> order = new ArrayList<>();
        coordinator.registerMain("zulu", () -> order.add("main-zulu"));
        coordinator.registerClient("zulu", () -> order.add("client-zulu"));
        coordinator.registerMain("alpha", () -> order.add("main-alpha"));
        coordinator.registerClient("alpha", () -> order.add("client-alpha"));

        assertThat(coordinator.invokeIfInitializationEvent(
                "net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent",
                () -> order.add("wrong-open"))).isFalse();
        assertThat(coordinator.invokeIfInitializationEvent(
                "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent",
                () -> order.add("open"))).isTrue();
        assertThat(coordinator.invokeIfInitializationEvent(
                "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent",
                () -> order.add("open-again"))).isFalse();

        assertThat(order).containsExactly(
                "open", "main-alpha", "main-zulu", "client-alpha", "client-zulu");
    }

    @Test
    void clientRecipeBookEventCanInitializeBeforeItsSnapshot() {
        var coordinator = new FabricRegistrationLifecycle.Coordinator();
        List<String> order = new ArrayList<>();
        coordinator.registerMain("fixture", () -> order.add("main"));
        coordinator.registerClient("fixture", () -> order.add("client"));

        assertThat(coordinator.invokeIfInitializationEvent(
                "net.minecraftforge.client.event.RegisterRecipeBookCategoriesEvent",
                () -> order.add("open"))).isTrue();

        assertThat(order).containsExactly("open", "main", "client");
    }

    @Test
    void invokesDedicatedServerEntrypointsInCanonicalModOrderOnce() {
        var coordinator = new FabricRegistrationLifecycle.ServerCoordinator();
        List<String> order = new ArrayList<>();
        coordinator.register("zulu", () -> order.add("server-zulu"));
        coordinator.register("alpha", () -> order.add("server-alpha"));

        assertThat(coordinator.invokeIfServerSetupEvent(
                "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent")).isFalse();
        assertThat(coordinator.invokeIfServerSetupEvent(
                "net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent")).isTrue();
        assertThat(coordinator.invokeIfServerSetupEvent(
                "net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent")).isFalse();

        assertThat(order).containsExactly("server-alpha", "server-zulu");
    }
}
