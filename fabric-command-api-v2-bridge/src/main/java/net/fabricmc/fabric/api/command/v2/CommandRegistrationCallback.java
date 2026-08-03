package net.fabricmc.fabric.api.command.v2;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Binary-compatible command-registration contract for Fabric Command API v2 2.2.28. */
@FunctionalInterface
public interface CommandRegistrationCallback {
    Event<CommandRegistrationCallback> EVENT = EventFactory.createArrayBacked(
            CommandRegistrationCallback.class, callbacks -> (dispatcher, registryAccess, environment) -> {
                for (CommandRegistrationCallback callback : callbacks) {
                    callback.register(dispatcher, registryAccess, environment);
                }
            });

    void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment);
}
