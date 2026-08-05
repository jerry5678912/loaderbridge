package net.fabricmc.fabric.api.tag;

import dev.loaderbridge.fabric.api.tag.convention.FabricTagKeySupport;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;

public interface FabricTagKey {
    default String getTranslationKey() {
        return FabricTagKeySupport.translationKey((TagKey<?>) (Object) this);
    }

    default Component getName() {
        return FabricTagKeySupport.name((TagKey<?>) (Object) this);
    }
}
