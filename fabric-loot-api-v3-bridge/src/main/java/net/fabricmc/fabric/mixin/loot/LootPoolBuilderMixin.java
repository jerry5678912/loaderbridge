package net.fabricmc.fabric.mixin.loot;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import net.fabricmc.fabric.api.loot.v3.FabricLootPoolBuilder;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LootPool.Builder.class)
abstract class LootPoolBuilderMixin implements FabricLootPoolBuilder {
    @Shadow @Final private ImmutableList.Builder<LootPoolEntryContainer> entries;
    @Shadow @Final private ImmutableList.Builder<LootItemCondition> conditions;
    @Shadow @Final private ImmutableList.Builder<LootItemFunction> functions;

    @Unique private LootPool.Builder loaderbridge$self() { return (LootPool.Builder) (Object) this; }

    @Override public LootPool.Builder with(LootPoolEntryContainer entry) { entries.add(entry); return loaderbridge$self(); }
    @Override public LootPool.Builder with(Collection<? extends LootPoolEntryContainer> values) { entries.addAll(values); return loaderbridge$self(); }
    @Override public LootPool.Builder conditionally(LootItemCondition condition) { conditions.add(condition); return loaderbridge$self(); }
    @Override public LootPool.Builder conditionally(Collection<? extends LootItemCondition> values) { conditions.addAll(values); return loaderbridge$self(); }
    @Override public LootPool.Builder apply(LootItemFunction function) { functions.add(function); return loaderbridge$self(); }
    @Override public LootPool.Builder apply(Collection<? extends LootItemFunction> values) { functions.addAll(values); return loaderbridge$self(); }
}
