package net.fabricmc.fabric.api.message.v1;

import java.util.Objects;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Phased server message decoration contract from Fabric Message API v1. */
public final class ServerMessageDecoratorEvent {
    public static final ResourceLocation CONTENT_PHASE =
            ResourceLocation.fromNamespaceAndPath("fabric", "content");
    public static final ResourceLocation STYLING_PHASE =
            ResourceLocation.fromNamespaceAndPath("fabric", "styling");
    public static final Event<ChatDecorator> EVENT = EventFactory.createWithPhases(
            ChatDecorator.class, decorators -> (sender, message) -> {
                Component decorated = message;
                for (ChatDecorator decorator : decorators) {
                    decorated = Objects.requireNonNull(decorator.decorate(sender, decorated),
                            "message decorator " + decorator.getClass().getName()
                                    + " returned null");
                }
                return decorated;
            }, CONTENT_PHASE, Event.DEFAULT_PHASE, STYLING_PHASE);

    private ServerMessageDecoratorEvent() {}
}
