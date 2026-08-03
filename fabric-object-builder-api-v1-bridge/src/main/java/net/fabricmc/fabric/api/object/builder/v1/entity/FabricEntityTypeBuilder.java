package net.fabricmc.fabric.api.object.builder.v1.entity;

import dev.loaderbridge.fabric.api.object.builder.SpawnPlacementBridge;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

/** Binary-compatible implementation of Fabric's deprecated 1.21.1 entity builder. */
@Deprecated
public class FabricEntityTypeBuilder<T extends Entity> {
    private MobCategory category;
    private EntityType.EntityFactory<T> factory;
    private boolean saveable = true;
    private boolean summonable = true;
    private int trackRange = 5;
    private int updateRate = 3;
    private Boolean velocityUpdates;
    private boolean fireImmune;
    private boolean spawnableFarFromPlayer;
    private EntityDimensions dimensions = EntityDimensions.scalable(-1.0F, -1.0F);
    private final Set<Block> spawnBlocks = new LinkedHashSet<>();
    private FeatureFlag[] requiredFeatures;

    protected FabricEntityTypeBuilder(MobCategory category, EntityType.EntityFactory<T> factory) {
        this.category = Objects.requireNonNull(category, "Spawn group cannot be null");
        this.factory = Objects.requireNonNull(factory, "Entity Factory cannot be null");
        this.spawnableFarFromPlayer = category == MobCategory.CREATURE || category == MobCategory.MISC;
    }

    @Deprecated
    public static <T extends Entity> FabricEntityTypeBuilder<T> create() {
        return create(MobCategory.MISC);
    }

    @Deprecated
    public static <T extends Entity> FabricEntityTypeBuilder<T> create(MobCategory category) {
        return create(category, FabricEntityTypeBuilder::emptyFactory);
    }

    @Deprecated
    public static <T extends Entity> FabricEntityTypeBuilder<T> create(MobCategory category,
            EntityType.EntityFactory<T> factory) {
        return new FabricEntityTypeBuilder<>(category, factory);
    }

    @Deprecated
    public static <T extends LivingEntity> Living<T> createLiving() {
        return new Living<>(MobCategory.MISC, FabricEntityTypeBuilder::emptyFactory);
    }

    @Deprecated
    public static <T extends net.minecraft.world.entity.Mob> Mob<T> createMob() {
        return new Mob<>(MobCategory.MISC, FabricEntityTypeBuilder::emptyFactory);
    }

    private static <T extends Entity> T emptyFactory(EntityType<T> type, Level level) {
        return null;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> spawnGroup(MobCategory category) {
        this.category = Objects.requireNonNull(category, "Spawn group cannot be null");
        return this;
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public <N extends T> FabricEntityTypeBuilder<N> entityFactory(
            EntityType.EntityFactory<N> factory) {
        this.factory = (EntityType.EntityFactory<T>) Objects.requireNonNull(
                factory, "Entity Factory cannot be null");
        return (FabricEntityTypeBuilder<N>) this;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> disableSummon() {
        summonable = false;
        return this;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> disableSaving() {
        saveable = false;
        return this;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> fireImmune() {
        fireImmune = true;
        return this;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> spawnableFarFromPlayer() {
        spawnableFarFromPlayer = true;
        return this;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> dimensions(EntityDimensions dimensions) {
        this.dimensions = Objects.requireNonNull(dimensions, "Cannot set null dimensions");
        return this;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> trackable(int trackRangeBlocks, int updateRate) {
        return trackable(trackRangeBlocks, updateRate, true);
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> trackable(int trackRangeBlocks, int updateRate,
            boolean velocityUpdates) {
        return trackRangeBlocks(trackRangeBlocks).trackedUpdateRate(updateRate)
                .forceTrackedVelocityUpdates(velocityUpdates);
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> trackRangeChunks(int range) {
        trackRange = range;
        return this;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> trackRangeBlocks(int range) {
        return trackRangeChunks((range + 15) / 16);
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> trackedUpdateRate(int rate) {
        updateRate = rate;
        return this;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> forceTrackedVelocityUpdates(boolean force) {
        velocityUpdates = force;
        return this;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> specificSpawnBlocks(Block... blocks) {
        spawnBlocks.clear();
        spawnBlocks.addAll(Arrays.asList(blocks));
        return this;
    }

    @Deprecated
    public FabricEntityTypeBuilder<T> requires(FeatureFlag... requiredFeatures) {
        this.requiredFeatures = requiredFeatures;
        return this;
    }

    @Deprecated
    public EntityType<T> build() {
        EntityType.Builder<T> builder = EntityType.Builder.of(factory, category)
                .immuneTo(spawnBlocks.toArray(Block[]::new))
                .clientTrackingRange(trackRange)
                .updateInterval(updateRate)
                .sized(dimensions.width(), dimensions.height());
        if (!saveable) builder.noSave();
        if (!summonable) builder.noSummon();
        if (fireImmune) builder.fireImmune();
        if (spawnableFarFromPlayer) builder.canSpawnFarFromPlayer();
        if (requiredFeatures != null) builder.requiredFeatures(requiredFeatures);
        if (velocityUpdates != null) builder.setShouldReceiveVelocityUpdates(velocityUpdates);
        return builder.build(null);
    }

    /** Living-entity specialization with default-attribute registration. */
    @Deprecated
    public static class Living<T extends LivingEntity> extends FabricEntityTypeBuilder<T> {
        private Supplier<AttributeSupplier.Builder> defaultAttributes;

        protected Living(MobCategory category, EntityType.EntityFactory<T> factory) {
            super(category, factory);
        }

        @Override public Living<T> spawnGroup(MobCategory value) { super.spawnGroup(value); return this; }
        @Override @SuppressWarnings("unchecked") public <N extends T> Living<N> entityFactory(EntityType.EntityFactory<N> value) { super.entityFactory(value); return (Living<N>) this; }
        @Override public Living<T> disableSummon() { super.disableSummon(); return this; }
        @Override public Living<T> disableSaving() { super.disableSaving(); return this; }
        @Override public Living<T> fireImmune() { super.fireImmune(); return this; }
        @Override public Living<T> spawnableFarFromPlayer() { super.spawnableFarFromPlayer(); return this; }
        @Override public Living<T> dimensions(EntityDimensions value) { super.dimensions(value); return this; }
        @Override public Living<T> trackable(int range, int rate) { super.trackable(range, rate); return this; }
        @Override public Living<T> trackable(int range, int rate, boolean velocity) { super.trackable(range, rate, velocity); return this; }
        @Override public Living<T> trackRangeChunks(int value) { super.trackRangeChunks(value); return this; }
        @Override public Living<T> trackRangeBlocks(int value) { super.trackRangeBlocks(value); return this; }
        @Override public Living<T> trackedUpdateRate(int value) { super.trackedUpdateRate(value); return this; }
        @Override public Living<T> forceTrackedVelocityUpdates(boolean value) { super.forceTrackedVelocityUpdates(value); return this; }
        @Override public Living<T> specificSpawnBlocks(Block... value) { super.specificSpawnBlocks(value); return this; }

        @Deprecated
        public Living<T> defaultAttributes(Supplier<AttributeSupplier.Builder> attributes) {
            defaultAttributes = Objects.requireNonNull(attributes, "Cannot set null attribute builder");
            return this;
        }

        @Override
        public EntityType<T> build() {
            EntityType<T> type = super.build();
            if (defaultAttributes != null) {
                FabricDefaultAttributeRegistry.register(type, defaultAttributes.get());
            }
            return type;
        }
    }

    /** Mob specialization with native spawn-restriction registration. */
    @Deprecated
    public static class Mob<T extends net.minecraft.world.entity.Mob> extends Living<T> {
        private SpawnPlacementType placement;
        private Heightmap.Types heightmap;
        private SpawnPlacements.SpawnPredicate<T> predicate;

        protected Mob(MobCategory category, EntityType.EntityFactory<T> factory) {
            super(category, factory);
        }

        @Override public Mob<T> spawnGroup(MobCategory value) { super.spawnGroup(value); return this; }
        @Override @SuppressWarnings("unchecked") public <N extends T> Mob<N> entityFactory(EntityType.EntityFactory<N> value) { super.entityFactory(value); return (Mob<N>) this; }
        @Override public Mob<T> disableSummon() { super.disableSummon(); return this; }
        @Override public Mob<T> disableSaving() { super.disableSaving(); return this; }
        @Override public Mob<T> fireImmune() { super.fireImmune(); return this; }
        @Override public Mob<T> spawnableFarFromPlayer() { super.spawnableFarFromPlayer(); return this; }
        @Override public Mob<T> dimensions(EntityDimensions value) { super.dimensions(value); return this; }
        @Override public Mob<T> trackable(int range, int rate) { super.trackable(range, rate); return this; }
        @Override public Mob<T> trackable(int range, int rate, boolean velocity) { super.trackable(range, rate, velocity); return this; }
        @Override public Mob<T> trackRangeChunks(int value) { super.trackRangeChunks(value); return this; }
        @Override public Mob<T> trackRangeBlocks(int value) { super.trackRangeBlocks(value); return this; }
        @Override public Mob<T> trackedUpdateRate(int value) { super.trackedUpdateRate(value); return this; }
        @Override public Mob<T> forceTrackedVelocityUpdates(boolean value) { super.forceTrackedVelocityUpdates(value); return this; }
        @Override public Mob<T> specificSpawnBlocks(Block... value) { super.specificSpawnBlocks(value); return this; }
        @Override public Mob<T> defaultAttributes(Supplier<AttributeSupplier.Builder> value) { super.defaultAttributes(value); return this; }

        @Deprecated
        public Mob<T> spawnRestriction(SpawnPlacementType placement, Heightmap.Types heightmap,
                SpawnPlacements.SpawnPredicate<T> predicate) {
            this.placement = Objects.requireNonNull(placement, "Spawn location cannot be null.");
            this.heightmap = Objects.requireNonNull(heightmap, "Heightmap type cannot be null.");
            this.predicate = Objects.requireNonNull(predicate, "Spawn predicate cannot be null.");
            return this;
        }

        @Override
        public EntityType<T> build() {
            EntityType<T> type = super.build();
            if (predicate != null) {
                SpawnPlacementBridge.register(type, placement, heightmap, predicate);
            }
            return type;
        }
    }
}
