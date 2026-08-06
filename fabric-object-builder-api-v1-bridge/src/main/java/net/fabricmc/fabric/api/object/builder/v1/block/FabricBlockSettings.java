package net.fabricmc.fabric.api.object.builder.v1.block;

import dev.loaderbridge.fabric.api.object.builder.mixin.BlockBehaviourPropertiesAccessor;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootTable;

/** Deprecated Fabric block properties retained for binary compatibility. */
@Deprecated
@SuppressWarnings("this-escape")
public class FabricBlockSettings extends BlockBehaviour.Properties {
    protected FabricBlockSettings() {
        super();
    }

    protected FabricBlockSettings(BlockBehaviour.Properties settings) {
        this();
        BlockBehaviourPropertiesAccessor source =
                (BlockBehaviourPropertiesAccessor) (Object) settings;
        BlockBehaviourPropertiesAccessor target =
                (BlockBehaviourPropertiesAccessor) (Object) this;
        target.loaderbridge$setMapColor(source.loaderbridge$getMapColor());
        target.loaderbridge$setHasCollision(source.loaderbridge$getHasCollision());
        target.loaderbridge$setSoundType(source.loaderbridge$getSoundType());
        target.loaderbridge$setLightEmission(source.loaderbridge$getLightEmission());
        target.loaderbridge$setExplosionResistance(source.loaderbridge$getExplosionResistance());
        target.loaderbridge$setDestroyTime(source.loaderbridge$getDestroyTime());
        target.loaderbridge$setRequiresCorrectToolForDrops(
                source.loaderbridge$getRequiresCorrectToolForDrops());
        target.loaderbridge$setIsRandomlyTicking(source.loaderbridge$getIsRandomlyTicking());
        target.loaderbridge$setFriction(source.loaderbridge$getFriction());
        target.loaderbridge$setSpeedFactor(source.loaderbridge$getSpeedFactor());
        target.loaderbridge$setJumpFactor(source.loaderbridge$getJumpFactor());
        target.loaderbridge$setDrops(source.loaderbridge$getDrops());
        target.loaderbridge$setCanOcclude(source.loaderbridge$getCanOcclude());
        target.loaderbridge$setIsAir(source.loaderbridge$getIsAir());
        target.loaderbridge$setIgnitedByLava(source.loaderbridge$getIgnitedByLava());
        target.loaderbridge$setLiquid(source.loaderbridge$getLiquid());
        target.loaderbridge$setForceSolidOff(source.loaderbridge$getForceSolidOff());
        target.loaderbridge$setForceSolidOn(source.loaderbridge$getForceSolidOn());
        target.loaderbridge$setPushReaction(source.loaderbridge$getPushReaction());
        target.loaderbridge$setSpawnTerrainParticles(
                source.loaderbridge$getSpawnTerrainParticles());
        target.loaderbridge$setInstrument(source.loaderbridge$getInstrument());
        target.loaderbridge$setReplaceable(source.loaderbridge$getReplaceable());
        target.loaderbridge$setLootTableSupplier(source.loaderbridge$getLootTableSupplier());
        target.loaderbridge$setIsValidSpawn(source.loaderbridge$getIsValidSpawn());
        target.loaderbridge$setIsRedstoneConductor(source.loaderbridge$getIsRedstoneConductor());
        target.loaderbridge$setIsSuffocating(source.loaderbridge$getIsSuffocating());
        target.loaderbridge$setIsViewBlocking(source.loaderbridge$getIsViewBlocking());
        target.loaderbridge$setHasPostProcess(source.loaderbridge$getHasPostProcess());
        target.loaderbridge$setEmissiveRendering(source.loaderbridge$getEmissiveRendering());
        target.loaderbridge$setDynamicShape(source.loaderbridge$getDynamicShape());
        target.loaderbridge$setRequiredFeatures(source.loaderbridge$getRequiredFeatures());
        target.loaderbridge$setOffsetFunction(source.loaderbridge$getOffsetFunction());
    }

    public static FabricBlockSettings create() {
        return new FabricBlockSettings();
    }

    public static FabricBlockSettings of() {
        return create();
    }

    public static FabricBlockSettings copyOf(BlockBehaviour block) {
        return new FabricBlockSettings(block.properties());
    }

    public static FabricBlockSettings copyOf(BlockBehaviour.Properties settings) {
        return new FabricBlockSettings(settings);
    }

    @Override public FabricBlockSettings noCollission() { super.noCollission(); return this; }
    @Override public FabricBlockSettings noOcclusion() { super.noOcclusion(); return this; }
    @Override public FabricBlockSettings friction(float value) { super.friction(value); return this; }
    @Override public FabricBlockSettings speedFactor(float value) { super.speedFactor(value); return this; }
    @Override public FabricBlockSettings jumpFactor(float value) { super.jumpFactor(value); return this; }
    @Override public FabricBlockSettings sound(SoundType value) { super.sound(value); return this; }
    @Override public FabricBlockSettings lightLevel(ToIntFunction<BlockState> value) { super.lightLevel(value); return this; }
    @Override public FabricBlockSettings strength(float hardness, float resistance) { super.strength(hardness, resistance); return this; }
    @Override public FabricBlockSettings instabreak() { super.instabreak(); return this; }
    @Override public FabricBlockSettings strength(float value) { super.strength(value); return this; }
    @Override public FabricBlockSettings randomTicks() { super.randomTicks(); return this; }
    @Override public FabricBlockSettings dynamicShape() { super.dynamicShape(); return this; }
    @Override public FabricBlockSettings noLootTable() { super.noLootTable(); return this; }
    @Override public FabricBlockSettings dropsLike(Block block) { super.dropsLike(block); return this; }
    @Override public FabricBlockSettings air() { super.air(); return this; }
    @Override public FabricBlockSettings isValidSpawn(BlockBehaviour.StateArgumentPredicate<EntityType<?>> value) { super.isValidSpawn(value); return this; }
    @Override public FabricBlockSettings isRedstoneConductor(BlockBehaviour.StatePredicate value) { super.isRedstoneConductor(value); return this; }
    @Override public FabricBlockSettings isSuffocating(BlockBehaviour.StatePredicate value) { super.isSuffocating(value); return this; }
    @Override public FabricBlockSettings isViewBlocking(BlockBehaviour.StatePredicate value) { super.isViewBlocking(value); return this; }
    @Override public FabricBlockSettings hasPostProcess(BlockBehaviour.StatePredicate value) { super.hasPostProcess(value); return this; }
    @Override public FabricBlockSettings emissiveRendering(BlockBehaviour.StatePredicate value) { super.emissiveRendering(value); return this; }
    @Override public FabricBlockSettings requiresCorrectToolForDrops() { super.requiresCorrectToolForDrops(); return this; }
    @Override public FabricBlockSettings mapColor(MapColor value) { super.mapColor(value); return this; }
    @Override public FabricBlockSettings destroyTime(float value) { super.destroyTime(value); return this; }
    @Override public FabricBlockSettings explosionResistance(float value) { super.explosionResistance(value); return this; }
    @Override public FabricBlockSettings offsetType(BlockBehaviour.OffsetType value) { super.offsetType(value); return this; }
    @Override public FabricBlockSettings noTerrainParticles() { super.noTerrainParticles(); return this; }
    @Override public FabricBlockSettings requiredFeatures(FeatureFlag... values) { super.requiredFeatures(values); return this; }
    @Override public FabricBlockSettings mapColor(Function<BlockState, MapColor> value) { super.mapColor(value); return this; }
    @Override public FabricBlockSettings ignitedByLava() { super.ignitedByLava(); return this; }
    @Override public FabricBlockSettings liquid() { super.liquid(); return this; }
    @Override public FabricBlockSettings forceSolidOn() { super.forceSolidOn(); return this; }
    @Override public FabricBlockSettings forceSolidOff() { super.forceSolidOff(); return this; }
    @Override public FabricBlockSettings pushReaction(PushReaction value) { super.pushReaction(value); return this; }
    @Override public FabricBlockSettings instrument(NoteBlockInstrument value) { super.instrument(value); return this; }
    @Override public FabricBlockSettings replaceable() { super.replaceable(); return this; }

    public FabricBlockSettings noCollision() { return noCollission(); }
    public FabricBlockSettings nonOpaque() { return noOcclusion(); }
    public FabricBlockSettings slipperiness(float value) { return friction(value); }
    public FabricBlockSettings velocityMultiplier(float value) { return speedFactor(value); }
    public FabricBlockSettings jumpVelocityMultiplier(float value) { return jumpFactor(value); }
    public FabricBlockSettings sounds(SoundType value) { return sound(value); }
    public FabricBlockSettings luminance(ToIntFunction<BlockState> value) { return lightLevel(value); }
    public FabricBlockSettings breakInstantly() { return instabreak(); }
    public FabricBlockSettings ticksRandomly() { return randomTicks(); }
    public FabricBlockSettings dynamicBounds() { return dynamicShape(); }
    public FabricBlockSettings dropsNothing() { return noLootTable(); }
    public FabricBlockSettings allowsSpawning(BlockBehaviour.StateArgumentPredicate<EntityType<?>> value) { return isValidSpawn(value); }
    public FabricBlockSettings solidBlock(BlockBehaviour.StatePredicate value) { return isRedstoneConductor(value); }
    public FabricBlockSettings suffocates(BlockBehaviour.StatePredicate value) { return isSuffocating(value); }
    public FabricBlockSettings blockVision(BlockBehaviour.StatePredicate value) { return isViewBlocking(value); }
    public FabricBlockSettings postProcess(BlockBehaviour.StatePredicate value) { return hasPostProcess(value); }
    public FabricBlockSettings emissiveLighting(BlockBehaviour.StatePredicate value) { return emissiveRendering(value); }
    public FabricBlockSettings requiresTool() { return requiresCorrectToolForDrops(); }
    public FabricBlockSettings hardness(float value) { return destroyTime(value); }
    public FabricBlockSettings resistance(float value) { return explosionResistance(value); }
    public FabricBlockSettings offset(BlockBehaviour.OffsetType value) { return offsetType(value); }
    public FabricBlockSettings noBlockBreakParticles() { return noTerrainParticles(); }
    public FabricBlockSettings requires(FeatureFlag... values) { return requiredFeatures(values); }
    public FabricBlockSettings burnable() { return ignitedByLava(); }
    public FabricBlockSettings solid() { return forceSolidOn(); }
    public FabricBlockSettings notSolid() { return forceSolidOff(); }
    public FabricBlockSettings pistonBehavior(PushReaction value) { return pushReaction(value); }

    public FabricBlockSettings lightLevel(int value) { return luminance(value); }
    public FabricBlockSettings luminance(int value) { return lightLevel(ignored -> value); }
    public FabricBlockSettings drops(ResourceKey<LootTable> value) {
        ((BlockBehaviourPropertiesAccessor) (Object) this).loaderbridge$setDrops(value);
        return this;
    }
    public FabricBlockSettings materialColor(MapColor value) { return mapColor(value); }
    public FabricBlockSettings materialColor(DyeColor value) { return mapColor(value); }
    @Override public FabricBlockSettings mapColor(DyeColor value) { super.mapColor(value); return this; }
    public FabricBlockSettings collidable(boolean value) {
        ((BlockBehaviourPropertiesAccessor) (Object) this).loaderbridge$setHasCollision(value);
        return this;
    }
}
