package dev.loaderbridge.fabric.api.screenhandler.mixin;

import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuConstructor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleMenuProvider.class)
public interface SimpleMenuProviderAccessor {
    @Accessor("menuConstructor")
    MenuConstructor loaderbridge$getMenuConstructor();
}
