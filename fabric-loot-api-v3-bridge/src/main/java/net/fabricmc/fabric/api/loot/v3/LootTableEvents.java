package net.fabricmc.fabric.api.loot.v3;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/** Fabric Loot API v3 events, implemented against the Forge-hosted vanilla loader. */
public final class LootTableEvents {
    private LootTableEvents() {}

    public static final Event<Replace> REPLACE = EventFactory.createArrayBacked(
            Replace.class, listeners -> (key, original, source, registries) -> {
                for (Replace listener : listeners) {
                    LootTable replacement = listener.replaceLootTable(key, original, source, registries);
                    if (replacement != null) return replacement;
                }
                return null;
            });

    public static final Event<Modify> MODIFY = EventFactory.createArrayBacked(
            Modify.class, listeners -> (key, builder, source, registries) -> {
                for (Modify listener : listeners) {
                    listener.modifyLootTable(key, builder, source, registries);
                }
            });

    public static final Event<Loaded> ALL_LOADED = EventFactory.createArrayBacked(
            Loaded.class, listeners -> (manager, registry) -> {
                for (Loaded listener : listeners) listener.onLootTablesLoaded(manager, registry);
            });

    @FunctionalInterface
    public interface Replace {
        @Nullable LootTable replaceLootTable(ResourceKey<LootTable> key, LootTable original,
                LootTableSource source, HolderLookup.Provider registries);
    }

    @FunctionalInterface
    public interface Modify {
        void modifyLootTable(ResourceKey<LootTable> key, LootTable.Builder tableBuilder,
                LootTableSource source, HolderLookup.Provider registries);
    }

    @FunctionalInterface
    public interface Loaded {
        void onLootTablesLoaded(ResourceManager resourceManager, Registry<LootTable> lootRegistry);
    }
}
