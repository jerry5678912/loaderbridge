package dev.loaderbridge.fabric.api.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class FabricMessageContractTest {
    @Test
    void exposesPinnedCommonServerSurface() {
        var descriptor = new FabricMessageBridgeProvider().descriptor();

        assertThat(descriptor.contractVersion()).isEqualTo("fabric-message-api-v1:6.0.14");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-message-api-v1", "6.0.14+6ced4dd919");
        assertThat(descriptor.providedClasses()).contains(
                "net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent",
                "net.fabricmc.fabric.api.message.v1.ServerMessageEvents$CommandMessage");
    }

    @Test
    void allowGameListenersShortCircuitAndAcceptedMessagesNotify() {
        List<String> calls = new ArrayList<>();
        ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) -> {
            calls.add("allow-first");
            return false;
        });
        ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) -> {
            calls.add("allow-second");
            return true;
        });
        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) ->
                calls.add("notify"));

        boolean allowed = ServerMessageEvents.ALLOW_GAME_MESSAGE.invoker()
                .allowGameMessage(null, Component.literal("blocked"), false);

        assertThat(allowed).isFalse();
        assertThat(calls).containsExactly("allow-first");
    }

    @Test
    void decoratorsRespectContentDefaultStylingOrderAndRejectNull() {
        ServerMessageDecoratorEvent.EVENT.register(
                ServerMessageDecoratorEvent.STYLING_PHASE,
                (sender, message) -> message.copy().append("-styling"));
        ServerMessageDecoratorEvent.EVENT.register(
                ServerMessageDecoratorEvent.CONTENT_PHASE,
                (sender, message) -> message.copy().append("-content"));
        ServerMessageDecoratorEvent.EVENT.register(
                (sender, message) -> message.copy().append("-default"));

        assertThat(ServerMessageDecoratorEvent.EVENT.invoker()
                .decorate(null, Component.literal("base")).getString())
                .isEqualTo("base-content-default-styling");

        ServerMessageDecoratorEvent.EVENT.register(
                ServerMessageDecoratorEvent.STYLING_PHASE, (sender, message) -> null);
        assertThatThrownBy(() -> ServerMessageDecoratorEvent.EVENT.invoker()
                .decorate(null, Component.literal("base")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("message decorator");
    }
}
