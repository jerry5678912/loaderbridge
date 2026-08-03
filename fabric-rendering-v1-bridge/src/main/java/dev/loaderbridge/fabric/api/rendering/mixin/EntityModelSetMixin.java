package dev.loaderbridge.fabric.api.rendering.mixin;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolves Fabric layers registered after Forge's layer-definition event. */
@Mixin(EntityModelSet.class)
abstract class EntityModelSetMixin {
    @Inject(method = "bakeLayer", at = @At("HEAD"), cancellable = true)
    private void loaderbridge$bakeFabricLayer(ModelLayerLocation layer,
            CallbackInfoReturnable<ModelPart> callback) {
        ModelPart model = EntityModelLayerRegistry.bakeRegistered(layer);
        if (model != null) callback.setReturnValue(model);
    }
}
