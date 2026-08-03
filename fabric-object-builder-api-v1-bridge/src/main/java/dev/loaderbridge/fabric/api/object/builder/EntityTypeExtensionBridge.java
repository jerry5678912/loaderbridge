package dev.loaderbridge.fabric.api.object.builder;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.levelgen.Heightmap;

/** Stores modern Fabric builder extensions until Minecraft creates the entity type. */
public final class EntityTypeExtensionBridge {
    private EntityTypeExtensionBridge() {
    }

    public static <T extends LivingEntity> EntityType.Builder<T> createLiving(
            EntityType.EntityFactory<T> factory, MobCategory category,
            UnaryOperator<FabricEntityType.Builder.Living<T>> operator) {
        EntityType.Builder<T> builder = EntityType.Builder.of(factory, category);
        LivingHook<T> hook = new LivingHook<>();
        operator.apply(hook);
        extension(builder).loaderbridge$setBuildHook(hook);
        return builder;
    }

    public static <T extends net.minecraft.world.entity.Mob> EntityType.Builder<T> createMob(
            EntityType.EntityFactory<T> factory, MobCategory category,
            UnaryOperator<FabricEntityType.Builder.Mob<T>> operator) {
        EntityType.Builder<T> builder = EntityType.Builder.of(factory, category);
        MobHook<T> hook = new MobHook<>();
        operator.apply(hook);
        extension(builder).loaderbridge$setBuildHook(hook);
        return builder;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> EntityTypeBuilderExtension<T> extension(
            EntityType.Builder<T> builder) {
        return (EntityTypeBuilderExtension<T>) (Object) builder;
    }

    public interface BuildHook<T extends Entity> {
        void onBuild(EntityType<T> type);
    }

    private static class LivingHook<T extends LivingEntity>
            implements FabricEntityType.Builder.Living<T>, BuildHook<T> {
        private Supplier<AttributeSupplier.Builder> attributes;

        @Override
        public FabricEntityType.Builder.Living<T> defaultAttributes(
                Supplier<AttributeSupplier.Builder> attributes) {
            this.attributes = Objects.requireNonNull(attributes,
                    "Cannot set null attribute builder");
            return this;
        }

        @Override
        public void onBuild(EntityType<T> type) {
            if (attributes != null) {
                FabricDefaultAttributeRegistry.register(type, attributes.get());
            }
        }
    }

    private static final class MobHook<T extends net.minecraft.world.entity.Mob>
            extends LivingHook<T> implements FabricEntityType.Builder.Mob<T> {
        private SpawnPlacementType placement;
        private Heightmap.Types heightmap;
        private SpawnPlacements.SpawnPredicate<T> predicate;

        @Override
        public FabricEntityType.Builder.Mob<T> defaultAttributes(
                Supplier<AttributeSupplier.Builder> attributes) {
            super.defaultAttributes(attributes);
            return this;
        }

        @Override
        public FabricEntityType.Builder.Mob<T> spawnRestriction(SpawnPlacementType placement,
                Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
            this.placement = Objects.requireNonNull(placement, "Location cannot be null.");
            this.heightmap = Objects.requireNonNull(heightmap, "Heightmap type cannot be null.");
            this.predicate = Objects.requireNonNull(predicate, "Spawn predicate cannot be null.");
            return this;
        }

        @Override
        public void onBuild(EntityType<T> type) {
            super.onBuild(type);
            if (predicate != null) {
                SpawnPlacementBridge.register(type, placement, heightmap, predicate);
            }
        }
    }
}
