package dev.loaderbridge.fabric.api.resource.conditions.mixin;

import com.google.gson.JsonElement;
import dev.loaderbridge.fabric.api.resource.conditions.BridgeResourceConditions;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimplePreparableReloadListener.class)
public abstract class SimplePreparableReloadListenerMixin {
    @Inject(method = "lambda$reload$1", at = @At("HEAD"))
    private void loaderbridge$filterFabricConditions(
            ResourceManager resourceManager,
            ProfilerFiller profiler,
            Object prepared,
            CallbackInfo callback) {
        Object listener = this;
        if (!(listener instanceof SimpleJsonResourceReloadListener)
                || !(prepared instanceof Map<?, ?> resources)) {
            return;
        }

        HolderLookup.Provider lookup = null;
        if (listener instanceof RecipeManagerAccessor recipes) {
            lookup = recipes.loaderbridge$getRegistries();
        } else if (listener instanceof ServerAdvancementManagerAccessor advancements) {
            lookup = advancements.loaderbridge$getRegistries();
        }

        HolderLookup.Provider finalLookup = lookup;
        Iterator<? extends Map.Entry<?, ?>> iterator = resources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            if (entry.getKey() instanceof ResourceLocation
                    && entry.getValue() instanceof JsonElement element
                    && element.isJsonObject()
                    && !BridgeResourceConditions.test(element.getAsJsonObject(), finalLookup)) {
                iterator.remove();
            }
        }
    }
}
