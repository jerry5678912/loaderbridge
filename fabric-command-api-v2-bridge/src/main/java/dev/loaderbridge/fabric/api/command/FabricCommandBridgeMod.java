package dev.loaderbridge.fabric.api.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

/** Publishes Forge command-registration events through the Fabric callback contract. */
@Mod("loaderbridge_fabric_command_v2")
public final class FabricCommandBridgeMod {
    public FabricCommandBridgeMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandRegistrationCallback.EVENT.invoker().register(
                event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }
}
