package dev.loaderbridge.fabric.api.item;

import java.util.List;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentSource;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import dev.loaderbridge.fabric.api.item.mixin.EnchantmentBuilderAccessor;

public final class EnchantingBridge {
    private static final ThreadLocal<Holder<Enchantment>> CAPTURED_ENCHANTMENT =
            new ThreadLocal<>();

    private EnchantingBridge() { }

    public static boolean canEnchant(Holder<Enchantment> enchantment,
            ItemStack stack, EnchantingContext context) {
        return ((FabricItemStack) (Object) stack).canBeEnchantedWith(enchantment, context);
    }

    public static void capture(Holder<Enchantment> enchantment) {
        CAPTURED_ENCHANTMENT.set(enchantment);
    }

    public static Holder<Enchantment> consumeCapture() {
        Holder<Enchantment> enchantment = CAPTURED_ENCHANTMENT.get();
        CAPTURED_ENCHANTMENT.remove();
        return enchantment;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Enchantment modify(ResourceKey<Enchantment> key,
            Enchantment original, EnchantmentSource source) {
        Enchantment.Builder builder = Enchantment.enchantment(original.definition());
        EnchantmentBuilderAccessor accessor = (EnchantmentBuilderAccessor) builder;
        builder.exclusiveWith(original.exclusiveSet());
        accessor.loaderbridge$getEffectMapBuilder().addAll(original.effects());
        original.effects().stream().forEach(component -> {
            if (component.value() instanceof List<?> values) {
                accessor.loaderbridge$getEffectsList((DataComponentType) component.type())
                        .addAll(values);
            }
        });
        EnchantmentEvents.MODIFY.invoker().modify(key, builder, source);
        return new Enchantment(original.description(),
                accessor.loaderbridge$getDefinition(),
                accessor.loaderbridge$getExclusiveSet(),
                accessor.loaderbridge$getEffectMapBuilder().build());
    }

    public static EnchantmentSource source(Resource resource) {
        if (resource.knownPackInfo().filter(pack -> pack.isVanilla()).isPresent()
                || resource.sourcePackId().equals("vanilla")) {
            return EnchantmentSource.VANILLA;
        }
        String packId = resource.sourcePackId();
        return packId.startsWith("mod:") || packId.equals("mod_resources")
                ? EnchantmentSource.MOD : EnchantmentSource.DATA_PACK;
    }
}
