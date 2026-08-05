package net.fabricmc.fabric.api.screenhandler.v1;

/** Fabric's interface-injected extension for controlling replacement menus. */
public interface FabricScreenHandlerFactory {
    default boolean shouldCloseCurrentScreen() {
        return true;
    }
}
