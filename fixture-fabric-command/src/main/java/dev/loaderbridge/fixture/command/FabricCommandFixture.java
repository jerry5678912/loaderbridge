package dev.loaderbridge.fixture.command;

import static net.minecraft.commands.Commands.literal;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** Registers a real Brigadier command through the Fabric callback. */
public final class FabricCommandFixture implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("loaderbridge_fabric_command").executes(context -> {
                    System.out.println("LOADERBRIDGE_FABRIC_COMMAND_EXECUTED");
                    return 1;
                })));
    }
}
