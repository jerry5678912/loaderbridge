package dev.loaderbridge.fabric.entity.events.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntitySharedFlagInvoker {
    @Invoker("setSharedFlag")
    void loaderbridge$setSharedFlag(int flag, boolean value);
}
