package net.fabricmc.fabric.api.tag.convention.v2;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ConventionalEnchantmentTags {
    public static final TagKey<Enchantment> INCREASE_BLOCK_DROPS = register("increase_block_drops");
    public static final TagKey<Enchantment> INCREASE_ENTITY_DROPS = register("increase_entity_drops");
    public static final TagKey<Enchantment> WEAPON_DAMAGE_ENHANCEMENTS = register("weapon_damage_enhancements");
    public static final TagKey<Enchantment> ENTITY_SPEED_ENHANCEMENTS = register("entity_speed_enhancements");
    public static final TagKey<Enchantment> ENTITY_AUXILIARY_MOVEMENT_ENHANCEMENTS = register("entity_auxiliary_movement_enhancements");
    public static final TagKey<Enchantment> ENTITY_DEFENSE_ENHANCEMENTS = register("entity_defense_enhancements");

    private ConventionalEnchantmentTags() { }

    private static TagKey<Enchantment> register(String path) {
        return TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
