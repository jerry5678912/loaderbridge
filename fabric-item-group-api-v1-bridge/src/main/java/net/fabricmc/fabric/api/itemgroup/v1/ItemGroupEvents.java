package net.fabricmc.fabric.api.itemgroup.v1;

import dev.loaderbridge.fabric.api.itemgroup.BridgeItemGroupEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public final class ItemGroupEvents {
    public static final Event<ModifyEntriesAll> MODIFY_ENTRIES_ALL =
            BridgeItemGroupEvents.allEvent();

    private ItemGroupEvents() {}

    public static Event<ModifyEntries> modifyEntriesEvent(ResourceKey<CreativeModeTab> group) {
        return BridgeItemGroupEvents.event(group);
    }

    @FunctionalInterface
    public interface ModifyEntries {
        void modifyEntries(FabricItemGroupEntries entries);
    }

    @FunctionalInterface
    public interface ModifyEntriesAll {
        void modifyEntries(CreativeModeTab group, FabricItemGroupEntries entries);
    }
}
