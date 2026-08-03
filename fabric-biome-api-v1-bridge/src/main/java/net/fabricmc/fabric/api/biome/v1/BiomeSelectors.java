package net.fabricmc.fabric.api.biome.v1;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;

public final class BiomeSelectors {
    private BiomeSelectors() { }

    public static Predicate<BiomeSelectionContext> all() { return context -> true; }
    public static Predicate<BiomeSelectionContext> vanilla() {
        return context -> "minecraft".equals(context.getBiomeKey().location().getNamespace());
    }
    public static Predicate<BiomeSelectionContext> foundInOverworld() {
        return context -> context.hasTag(net.minecraft.tags.BiomeTags.IS_OVERWORLD);
    }
    public static Predicate<BiomeSelectionContext> foundInTheNether() {
        return context -> context.hasTag(net.minecraft.tags.BiomeTags.IS_NETHER);
    }
    public static Predicate<BiomeSelectionContext> foundInTheEnd() {
        return context -> context.hasTag(net.minecraft.tags.BiomeTags.IS_END);
    }
    public static Predicate<BiomeSelectionContext> tag(TagKey<Biome> tag) {
        return context -> context.hasTag(tag);
    }

    @SafeVarargs
    public static Predicate<BiomeSelectionContext> excludeByKey(ResourceKey<Biome>... keys) {
        Set<ResourceKey<Biome>> selected = new HashSet<>();
        for (ResourceKey<Biome> key : keys) selected.add(key);
        return excludeByKey(selected);
    }
    public static Predicate<BiomeSelectionContext> excludeByKey(Collection<ResourceKey<Biome>> keys) {
        Set<ResourceKey<Biome>> selected = Set.copyOf(keys);
        return context -> !selected.contains(context.getBiomeKey());
    }
    @SafeVarargs
    public static Predicate<BiomeSelectionContext> includeByKey(ResourceKey<Biome>... keys) {
        Set<ResourceKey<Biome>> selected = new HashSet<>();
        for (ResourceKey<Biome> key : keys) selected.add(key);
        return includeByKey(selected);
    }
    public static Predicate<BiomeSelectionContext> includeByKey(Collection<ResourceKey<Biome>> keys) {
        Set<ResourceKey<Biome>> selected = Set.copyOf(keys);
        return context -> selected.contains(context.getBiomeKey());
    }
    public static Predicate<BiomeSelectionContext> spawnsOneOf(EntityType<?>... entityTypes) {
        return spawnsOneOf(Set.of(entityTypes));
    }
    public static Predicate<BiomeSelectionContext> spawnsOneOf(Set<EntityType<?>> entityTypes) {
        Set<EntityType<?>> selected = Set.copyOf(entityTypes);
        return context -> context.getBiome().getMobSettings().getEntityTypes().stream().anyMatch(selected::contains);
    }
}
