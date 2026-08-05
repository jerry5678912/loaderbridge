package net.fabricmc.fabric.impl.item;

import java.util.WeakHashMap;
import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.api.item.v1.EquipmentSlotProvider;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public final class FabricItemInternals {
    private static final WeakHashMap<Item.Properties, ExtraData> EXTRA_DATA = new WeakHashMap<>();
    private FabricItemInternals() { }

    public static ExtraData computeExtraData(Item.Properties properties) {
        return EXTRA_DATA.computeIfAbsent(properties, ignored -> new ExtraData());
    }

    public static void onBuild(Item.Properties properties, Item item) {
        ExtraData data = EXTRA_DATA.get(properties);
        if (data == null) return;
        ItemExtensions extensions = (ItemExtensions) item;
        extensions.fabric_setEquipmentSlotProvider(data.equipmentSlotProvider);
        extensions.fabric_setCustomDamageHandler(data.customDamageHandler);
    }

    public static final class ExtraData {
        private @Nullable EquipmentSlotProvider equipmentSlotProvider;
        private @Nullable CustomDamageHandler customDamageHandler;
        public void equipmentSlot(EquipmentSlotProvider provider) { equipmentSlotProvider = provider; }
        public void customDamage(CustomDamageHandler handler) { customDamageHandler = handler; }
    }
}
