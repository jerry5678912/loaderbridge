package net.fabricmc.fabric.api.loot.v3;

import java.util.Collection;
import net.fabricmc.fabric.mixin.loot.LootPoolAccessor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Binary-compatible convenience methods injected into {@link LootPool.Builder}. */
public interface FabricLootPoolBuilder {
    default LootPool.Builder with(LootPoolEntryContainer entry) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    default LootPool.Builder with(Collection<? extends LootPoolEntryContainer> entries) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    default LootPool.Builder conditionally(LootItemCondition condition) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    default LootPool.Builder conditionally(Collection<? extends LootItemCondition> conditions) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    default LootPool.Builder apply(LootItemFunction function) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    default LootPool.Builder apply(Collection<? extends LootItemFunction> functions) {
        throw new UnsupportedOperationException("Implemented via LoaderBridge mixin");
    }

    static LootPool.Builder copyOf(LootPool pool) {
        LootPoolAccessor accessor = (LootPoolAccessor) pool;
        LootPool.Builder builder = LootPool.lootPool()
                .setRolls(accessor.fabric_getRolls())
                .setBonusRolls(accessor.fabric_getBonusRolls());
        FabricLootPoolBuilder extension = (FabricLootPoolBuilder) (Object) builder;
        extension.with(accessor.fabric_getEntries());
        extension.conditionally(accessor.fabric_getConditions());
        extension.apply(accessor.fabric_getFunctions());
        return builder;
    }
}
