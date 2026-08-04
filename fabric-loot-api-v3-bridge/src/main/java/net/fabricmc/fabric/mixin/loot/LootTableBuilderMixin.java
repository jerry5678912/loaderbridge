package net.fabricmc.fabric.mixin.loot;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.loot.v3.FabricLootPoolBuilder;
import net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LootTable.Builder.class)
abstract class LootTableBuilderMixin implements FabricLootTableBuilder {
    @Shadow @Final @Mutable private ImmutableList.Builder<LootPool> pools;
    @Shadow @Final private ImmutableList.Builder<LootItemFunction> functions;

    @Unique private LootTable.Builder loaderbridge$self() { return (LootTable.Builder) (Object) this; }

    @Override public LootTable.Builder pool(LootPool pool) { pools.add(pool); return loaderbridge$self(); }
    @Override public LootTable.Builder apply(LootItemFunction function) { functions.add(function); return loaderbridge$self(); }
    @Override public LootTable.Builder pools(Collection<? extends LootPool> values) { pools.addAll(values); return loaderbridge$self(); }
    @Override public LootTable.Builder apply(Collection<? extends LootItemFunction> values) { functions.addAll(values); return loaderbridge$self(); }
    @Override public LootTable.Builder modifyPools(Consumer<? super LootPool.Builder> modifier) {
        var values = new ArrayList<>(pools.build());
        ListIterator<LootPool> iterator = values.listIterator();
        while (iterator.hasNext()) {
            LootPool.Builder builder = FabricLootPoolBuilder.copyOf(iterator.next());
            modifier.accept(builder);
            iterator.set(builder.build());
        }
        pools = ImmutableList.builder();
        pools.addAll(values);
        return loaderbridge$self();
    }
}
