package dev.loaderbridge.fabric.api.resource.conditions.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerAdvancementManager.class)
public interface ServerAdvancementManagerAccessor {
    @Accessor("registries")
    HolderLookup.Provider loaderbridge$getRegistries();
}
