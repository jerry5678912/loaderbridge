package net.fabricmc.fabric.api.tag.v1;

import java.util.List;
import net.minecraft.tags.TagEntry;

/** Fabric extension exposed on Minecraft's decoded tag-file record. */
public interface FabricTagFile {
    default List<TagEntry> remove() {
        throw new AssertionError("Implemented via mixin");
    }
}
