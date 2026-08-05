package dev.loaderbridge.fabric.api.item.mixin;

import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.api.item.v1.EquipmentSlotProvider;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.fabricmc.fabric.impl.item.FabricItemInternals;
import net.fabricmc.fabric.impl.item.ItemExtensions;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public abstract class ItemMixin implements ItemExtensions, FabricItem {
    @Unique private static final ThreadLocal<PendingRecipeRemainder>
            LOADERBRIDGE$PENDING_RECIPE_REMAINDER = new ThreadLocal<>();
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    @org.spongepowered.asm.mixin.Mutable private DataComponentMap components;
    @org.spongepowered.asm.mixin.Shadow private DataComponentMap builtComponents;
    @Unique private @Nullable EquipmentSlotProvider loaderbridge$equipmentSlotProvider;
    @Unique private @Nullable CustomDamageHandler loaderbridge$customDamageHandler;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void loaderbridge$copyFabricProperties(Item.Properties properties, CallbackInfo callback) {
        FabricItemInternals.onBuild(properties, (Item) (Object) this);
    }

    @Override public @Nullable EquipmentSlotProvider fabric_getEquipmentSlotProvider() {
        return loaderbridge$equipmentSlotProvider;
    }
    @Override public void fabric_setEquipmentSlotProvider(@Nullable EquipmentSlotProvider provider) {
        loaderbridge$equipmentSlotProvider = provider;
    }
    @Override public @Nullable CustomDamageHandler fabric_getCustomDamageHandler() {
        return loaderbridge$customDamageHandler;
    }
    @Override public void fabric_setCustomDamageHandler(@Nullable CustomDamageHandler handler) {
        loaderbridge$customDamageHandler = handler;
    }
    @Override public void fabric_setDefaultComponents(DataComponentMap components) {
        this.components = components;
        this.builtComponents = components;
    }

    public boolean hasCraftingRemainingItem(ItemStack stack) {
        ItemStack remainder = ((FabricItem) (Object) this).getRecipeRemainder(stack);
        if (remainder.isEmpty()) {
            LOADERBRIDGE$PENDING_RECIPE_REMAINDER.remove();
            return false;
        }
        LOADERBRIDGE$PENDING_RECIPE_REMAINDER.set(
                new PendingRecipeRemainder(stack, remainder));
        return true;
    }

    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        PendingRecipeRemainder pending = LOADERBRIDGE$PENDING_RECIPE_REMAINDER.get();
        LOADERBRIDGE$PENDING_RECIPE_REMAINDER.remove();
        return pending != null && pending.input() == stack
                ? pending.remainder()
                : ((FabricItem) (Object) this).getRecipeRemainder(stack);
    }

    @Unique
    private record PendingRecipeRemainder(ItemStack input, ItemStack remainder) { }
}
