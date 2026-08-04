package dev.loaderbridge.fixture.loot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;

/** Controlled behavioral fixture: cobblestone must drop both a diamond and an emerald. */
public final class FabricLootFixture implements ModInitializer {
    private static final ResourceKey<LootTable> TARGET = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("minecraft", "blocks/cobblestone"));

    @Override
    public void onInitialize() {
        LootTableEvents.REPLACE.register((key, original, source, registries) -> {
            if (!TARGET.equals(key)) return null;
            if (source != LootTableSource.VANILLA) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_LOOT_SOURCE_FAILED=" + source);
            }
            System.out.println("LOADERBRIDGE_FABRIC_LOOT_REPLACE_READY=" + source);
            return LootTable.lootTable()
                    .withPool(LootPool.lootPool().add(LootItem.lootTableItem(Items.DIAMOND)))
                    .build();
        });
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            if (!TARGET.equals(key)) return;
            if (source != LootTableSource.REPLACED) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_LOOT_REPLACED_SOURCE_FAILED=" + source);
            }
            ((FabricLootTableBuilder) builder).pool(
                    LootPool.lootPool().add(LootItem.lootTableItem(Items.EMERALD)).build());
            System.out.println("LOADERBRIDGE_FABRIC_LOOT_MODIFY_READY=" + source);
        });
        LootTableEvents.ALL_LOADED.register((resources, registry) -> {
            if (!registry.containsKey(TARGET.location())) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_LOOT_REGISTRY_FAILED");
            }
            System.out.println("LOADERBRIDGE_FABRIC_LOOT_ALL_LOADED_READY");
        });
        System.out.println("LOADERBRIDGE_FABRIC_LOOT_ENTRYPOINT_READY");
    }
}
