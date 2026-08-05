package dev.loaderbridge.fabric.api.item.mixin;

import net.fabricmc.fabric.api.item.v1.FabricTooltipType;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TooltipFlag.class)
public interface TooltipFlagMixin extends FabricTooltipType {
}
