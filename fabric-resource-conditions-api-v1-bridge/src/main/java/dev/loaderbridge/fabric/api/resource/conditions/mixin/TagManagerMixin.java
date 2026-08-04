package dev.loaderbridge.fabric.api.resource.conditions.mixin;

import dev.loaderbridge.fabric.api.resource.conditions.BridgeResourceConditions;
import java.util.List;
import net.minecraft.tags.TagManager;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TagManager.class)
public abstract class TagManagerMixin {
    @Shadow private List<TagManager.LoadResult<?>> results;

    @Dynamic
    @Inject(method = "lambda$reload$2", at = @At("RETURN"))
    private void loaderbridge$captureLoadedTags(List<?> ignored, Void unused, CallbackInfo callback) {
        BridgeResourceConditions.captureLoadedTags(results);
    }
}
