package dev.loaderbridge.fabric.api.object.builder.mixin;

import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives the Fabric contract the same mutable attribute table used by Forge. */
@Mixin(ForgeHooks.class)
public interface ForgeHooksAccessor {
    @Accessor("FORGE_ATTRIBUTES")
    static Map<EntityType<? extends LivingEntity>, AttributeSupplier>
            loaderbridge$getAttributes() {
        throw new AssertionError("Mixin accessor was not transformed");
    }
}
