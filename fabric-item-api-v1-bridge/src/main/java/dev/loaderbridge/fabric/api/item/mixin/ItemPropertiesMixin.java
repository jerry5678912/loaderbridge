package dev.loaderbridge.fabric.api.item.mixin;

import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.Properties.class)
public abstract class ItemPropertiesMixin implements FabricItem.Settings {
}
