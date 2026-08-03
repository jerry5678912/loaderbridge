package net.fabricmc.fabric.api.object.builder.v1.entity;

import dev.loaderbridge.fabric.api.object.builder.EntityTypeExtensionBridge;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.levelgen.Heightmap;

/** Fabric extensions injected onto Minecraft's entity type builder. */
public interface FabricEntityType {
    interface Builder<T extends Entity> {
        default EntityType.Builder<T> alwaysUpdateVelocity(boolean alwaysUpdateVelocity) {
            throw new AssertionError("Implemented in Mixin");
        }

        default EntityType<T> build() {
            throw new AssertionError("Implemented in Mixin");
        }

        static <T extends LivingEntity> EntityType.Builder<T> createLiving(
                EntityType.EntityFactory<T> factory, MobCategory category,
                UnaryOperator<Living<T>> livingBuilder) {
            return EntityTypeExtensionBridge.createLiving(factory, category, livingBuilder);
        }

        static <T extends net.minecraft.world.entity.Mob> EntityType.Builder<T> createMob(
                EntityType.EntityFactory<T> factory, MobCategory category,
                UnaryOperator<Mob<T>> mobBuilder) {
            return EntityTypeExtensionBridge.createMob(factory, category, mobBuilder);
        }

        interface Living<T extends LivingEntity> {
            Living<T> defaultAttributes(Supplier<AttributeSupplier.Builder> defaultAttributeBuilder);
        }

        interface Mob<T extends net.minecraft.world.entity.Mob> extends Living<T> {
            Mob<T> spawnRestriction(SpawnPlacementType placement, Heightmap.Types heightmap,
                    SpawnPlacements.SpawnPredicate<T> predicate);

            @Override
            Mob<T> defaultAttributes(Supplier<AttributeSupplier.Builder> defaultAttributeBuilder);
        }
    }
}
