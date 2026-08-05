package dev.loaderbridge.fabric.api.item.mixin;

import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.api.item.v1.EquipmentSlotProvider;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.fabricmc.fabric.impl.item.FabricItemInternals;
import net.fabricmc.fabric.impl.item.ItemExtensions;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponentMap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public abstract class ItemMixin implements ItemExtensions, FabricItem {
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
}
