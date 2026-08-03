package dev.loaderbridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FabricRegistrationLifecycleTest {
    @Test
    void invokesResolvedEntrypointsTogetherDuringCommonSetup() {
        var coordinator = new FabricRegistrationLifecycle.Coordinator();
        List<String> order = new ArrayList<>();
        coordinator.registerMain(() -> order.add("main-one"));
        coordinator.registerClient(() -> order.add("client-one"));
        coordinator.registerMain(() -> order.add("main-two"));
        coordinator.registerClient(() -> order.add("client-two"));

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
                "open", "main-one", "main-two", "client-one", "client-two");
    }

    @Test
    void clientRecipeBookEventCanInitializeBeforeItsSnapshot() {
        var coordinator = new FabricRegistrationLifecycle.Coordinator();
        List<String> order = new ArrayList<>();
        coordinator.registerMain(() -> order.add("main"));
        coordinator.registerClient(() -> order.add("client"));

        assertThat(coordinator.invokeIfInitializationEvent(
                "net.minecraftforge.client.event.RegisterRecipeBookCategoriesEvent",
                () -> order.add("open"))).isTrue();

        assertThat(order).containsExactly("open", "main", "client");
    }
}
