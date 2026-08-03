package dev.loaderbridge.fabric.api.object.builder.mixin;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import net.minecraft.Util;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Matches Fabric's allowance for unnamed entity types built before registration. */
@Mixin(EntityType.Builder.class)
public abstract class EntityTypeBuilderNullIdMixin {
    @Redirect(method = "build", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/Util;fetchChoiceType(Lcom/mojang/datafixers/DSL$TypeReference;"
                    + "Ljava/lang/String;)Lcom/mojang/datafixers/types/Type;"))
    private Type<?> loaderbridge$allowNullId(DSL.TypeReference reference, String id) {
        return id == null ? null : Util.fetchChoiceType(reference, id);
    }
}
