package net.fabricmc.fabric.api.loot.v3;

import java.util.Collection;
import java.util.function.Consumer;
import net.fabricmc.fabric.mixin.loot.LootTableAccessor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

/** Binary-compatible convenience methods injected into {@link LootTable.Builder}. */
public interface FabricLootTableBuilder {
    default LootTable.Builder pool(LootPool pool) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    default LootTable.Builder apply(LootItemFunction function) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    default LootTable.Builder pools(Collection<? extends LootPool> pools) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    default LootTable.Builder apply(Collection<? extends LootItemFunction> functions) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    default LootTable.Builder modifyPools(Consumer<? super LootPool.Builder> modifier) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    static LootTable.Builder copyOf(LootTable table) {
        LootTableAccessor accessor = (LootTableAccessor) table;
        LootTable.Builder builder = LootTable.lootTable().setParamSet(table.getParamSet());
        FabricLootTableBuilder extension = (FabricLootTableBuilder) (Object) builder;
        extension.pools(accessor.fabric_getPools());
        extension.apply(accessor.fabric_getFunctions());
        accessor.fabric_getRandomSequence().ifPresent(builder::setRandomSequence);
        return builder;
    }
}
