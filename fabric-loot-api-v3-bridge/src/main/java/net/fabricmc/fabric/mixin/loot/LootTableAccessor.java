package net.fabricmc.fabric.mixin.loot;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootTable.class)
public interface LootTableAccessor {
    @Accessor("pools") List<LootPool> fabric_getPools();
    @Accessor("functions") List<LootItemFunction> fabric_getFunctions();
    @Accessor("randomSequence") Optional<ResourceLocation> fabric_getRandomSequence();
}
