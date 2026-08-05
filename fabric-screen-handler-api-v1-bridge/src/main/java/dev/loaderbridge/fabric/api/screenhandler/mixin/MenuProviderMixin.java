package dev.loaderbridge.fabric.api.screenhandler.mixin;

import net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MenuProvider.class)
public interface MenuProviderMixin extends FabricScreenHandlerFactory { }
