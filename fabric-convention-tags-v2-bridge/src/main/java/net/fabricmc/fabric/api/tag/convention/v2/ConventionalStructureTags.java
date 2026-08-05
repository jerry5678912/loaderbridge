package net.fabricmc.fabric.api.tag.convention.v2;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class ConventionalStructureTags {
    public static final TagKey<Structure> HIDDEN_FROM_DISPLAYERS = register("hidden_from_displayers");
    public static final TagKey<Structure> HIDDEN_FROM_LOCATOR_SELECTION = register("hidden_from_locator_selection");

    private ConventionalStructureTags() { }

    private static TagKey<Structure> register(String path) {
        return TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
