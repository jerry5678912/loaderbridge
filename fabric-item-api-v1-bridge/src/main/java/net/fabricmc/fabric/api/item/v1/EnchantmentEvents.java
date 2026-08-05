package net.fabricmc.fabric.api.item.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EnchantmentEvents {
    public static final Event<AllowEnchanting> ALLOW_ENCHANTING = EventFactory.createArrayBacked(
            AllowEnchanting.class, callbacks -> (enchantment, target, context) -> {
                for (AllowEnchanting callback : callbacks) {
                    TriState result = callback.allowEnchanting(enchantment, target, context);
                    if (result != TriState.DEFAULT) return result;
                }
                return TriState.DEFAULT;
            });
    public static final Event<Modify> MODIFY = EventFactory.createArrayBacked(
            Modify.class, callbacks -> (key, builder, source) -> {
                for (Modify callback : callbacks) callback.modify(key, builder, source);
            });

    private EnchantmentEvents() { }

    @FunctionalInterface
    public interface AllowEnchanting {
        TriState allowEnchanting(Holder<Enchantment> enchantment, ItemStack target,
                EnchantingContext enchantingContext);
    }

    @FunctionalInterface
    public interface Modify {
        void modify(ResourceKey<Enchantment> key, Enchantment.Builder builder,
                EnchantmentSource source);
    }
}
