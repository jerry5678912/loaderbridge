package net.fabricmc.fabric.api.itemgroup.v1;

import net.minecraft.world.item.CreativeModeTab;

/** Fabric's neutral custom creative-tab builder entrypoint. */
public final class FabricItemGroup {
    private FabricItemGroup() {}

    public static CreativeModeTab.Builder builder() {
        return CreativeModeTab.builder();
    }
}
