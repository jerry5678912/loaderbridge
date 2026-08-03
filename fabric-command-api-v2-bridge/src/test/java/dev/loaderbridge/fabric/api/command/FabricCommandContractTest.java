package dev.loaderbridge.fabric.api.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.junit.jupiter.api.Test;

class FabricCommandContractTest {
    @Test
    void callbackEventInvokesListenersInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> calls.add("first"));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> calls.add("second"));

        CommandRegistrationCallback.EVENT.invoker().register(null, null, null);

        assertThat(calls).containsExactly("first", "second");
    }

    @Test
    void providerPinsExactFabricContractAndBaseRequirement() {
        var descriptor = new FabricCommandBridgeProvider().descriptor();

        assertThat(descriptor.contractVersion()).isEqualTo("fabric-command-api-v2:2.2.28");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-command-api-v2", "2.2.28+6ced4dd919");
        assertThat(descriptor.requiredModules()).containsExactly("fabric-api-base-bridge");
        assertThat(descriptor.providedClasses())
                .containsExactly("net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback");
    }
}
