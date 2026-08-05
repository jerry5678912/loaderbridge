package dev.loaderbridge.fabric.api.item.mixin;

import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Enchantment.Builder.class)
public interface EnchantmentBuilderAccessor {
    @Accessor("definition")
    Enchantment.EnchantmentDefinition loaderbridge$getDefinition();

    @Accessor("exclusiveSet")
    HolderSet<Enchantment> loaderbridge$getExclusiveSet();

    @Accessor("effectMapBuilder")
    DataComponentMap.Builder loaderbridge$getEffectMapBuilder();

    @Invoker("getEffectsList")
    <E> List<E> loaderbridge$getEffectsList(DataComponentType<List<E>> type);
}
