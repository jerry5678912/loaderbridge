package dev.loaderbridge.fabric.api.itemgroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

public final class BridgeItemGroupEvents {
    private static final Map<ResourceKey<CreativeModeTab>, Event<ItemGroupEvents.ModifyEntries>> EVENTS =
            new LinkedHashMap<>();
    private static final Event<ItemGroupEvents.ModifyEntriesAll> ALL = EventFactory.createArrayBacked(
            ItemGroupEvents.ModifyEntriesAll.class, listeners -> (group, entries) -> {
                for (var listener : listeners) listener.modifyEntries(group, entries);
            });

    private BridgeItemGroupEvents() {}

    public static synchronized Event<ItemGroupEvents.ModifyEntries> event(
            ResourceKey<CreativeModeTab> key) {
        return EVENTS.computeIfAbsent(key, ignored -> EventFactory.createArrayBacked(
                ItemGroupEvents.ModifyEntries.class, listeners -> entries -> {
                    for (var listener : listeners) listener.modifyEntries(entries);
                }));
    }

    public static Event<ItemGroupEvents.ModifyEntriesAll> allEvent() { return ALL; }

    public static void apply(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> display = new ArrayList<>();
        List<ItemStack> search = new ArrayList<>();
        for (var entry : event.getEntries()) {
            if (entry.getValue() != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY) {
                display.add(entry.getKey());
            }
            if (entry.getValue() != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY) {
                search.add(entry.getKey());
            }
        }
        FabricItemGroupEntries entries = new FabricItemGroupEntries(event.getParameters(), display, search);
        Event<ItemGroupEvents.ModifyEntries> keyed;
        synchronized (BridgeItemGroupEvents.class) { keyed = EVENTS.get(event.getTabKey()); }
        if (keyed != null) keyed.invoker().modifyEntries(entries);
        ALL.invoker().modifyEntries(event.getTab(), entries);

        List<ItemStack> existing = new ArrayList<>();
        event.getEntries().forEach(entry -> existing.add(entry.getKey()));
        existing.forEach(event.getEntries()::remove);
        for (ItemStack stack : display) {
            event.accept(stack, contains(search, stack)
                    ? CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                    : CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
        for (ItemStack stack : search) {
            if (!contains(display, stack)) {
                event.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
            }
        }
    }

    private static boolean contains(List<ItemStack> stacks, ItemStack candidate) {
        return stacks.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, candidate));
    }
}
