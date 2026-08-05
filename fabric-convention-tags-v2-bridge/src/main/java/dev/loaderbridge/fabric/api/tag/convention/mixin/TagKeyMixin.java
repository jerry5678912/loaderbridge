package dev.loaderbridge.fabric.api.tag.convention.mixin;

import net.fabricmc.fabric.api.tag.FabricTagKey;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TagKey.class)
public interface TagKeyMixin extends FabricTagKey { }
