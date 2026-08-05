package net.fabricmc.fabric.impl.item;

import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.api.item.v1.EquipmentSlotProvider;
import org.jetbrains.annotations.Nullable;

public interface ItemExtensions {
    @Nullable EquipmentSlotProvider fabric_getEquipmentSlotProvider();
    void fabric_setEquipmentSlotProvider(@Nullable EquipmentSlotProvider provider);
    @Nullable CustomDamageHandler fabric_getCustomDamageHandler();
    void fabric_setCustomDamageHandler(@Nullable CustomDamageHandler handler);
}
