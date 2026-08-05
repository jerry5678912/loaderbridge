package dev.loaderbridge.fabric.api.tag.v1.mixin;

import net.fabricmc.fabric.api.tag.v1.FabricTagFile;
import net.minecraft.tags.TagFile;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TagFile.class)
public interface TagFileMixin extends FabricTagFile { }
