package dev.loaderbridge.fabric.entity.events.mixin;

import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow public abstract boolean isDeadOrDying();
    @Shadow public abstract Optional<BlockPos> getSleepingPos();

    @Redirect(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;isDeadOrDying()Z", ordinal = 1))
    private boolean loaderbridge$allowDeath(LivingEntity entity,
            DamageSource source, float amount) {
        if (entity.level().isClientSide) return entity.isDeadOrDying();
        return entity.isDeadOrDying()
                && ServerLivingEntityEvents.ALLOW_DEATH.invoker()
                        .allowDeath(entity, source, amount);
    }

    @Inject(method = "hurt", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void loaderbridge$afterDamage(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> callback, float baseDamage, boolean blocked) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide && !self.isDeadOrDying()) {
            ServerLivingEntityEvents.AFTER_DAMAGE.invoker()
                    .afterDamage(self, source, baseDamage, amount, blocked);
        }
    }

    @Redirect(method = "die", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;killedEntity("
                    + "Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean loaderbridge$afterKilledOtherEntity(Entity attacker,
            ServerLevel level, LivingEntity killed, DamageSource source) {
        boolean result = attacker.killedEntity(level, killed);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.invoker()
                .afterKilledOtherEntity(level, attacker, killed);
        return result;
    }

    @Inject(method = "die", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;broadcastEntityEvent("
                    + "Lnet/minecraft/world/entity/Entity;B)V"))
    private void loaderbridge$afterDeath(DamageSource source, CallbackInfo callback) {
        ServerLivingEntityEvents.AFTER_DEATH.invoker()
                .afterDeath((LivingEntity) (Object) this, source);
    }

    @Inject(method = "startSleeping", at = @At("RETURN"))
    private void loaderbridge$startSleeping(BlockPos position, CallbackInfo callback) {
        EntitySleepEvents.START_SLEEPING.invoker()
                .onStartSleeping((LivingEntity) (Object) this, position);
    }

    @Redirect(method = {"startSleeping", "lambda$stopSleeping$11"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isBed("
                            + "Lnet/minecraft/world/level/BlockGetter;"
                            + "Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean loaderbridge$allowBed(BlockState state, BlockGetter level,
            BlockPos position, LivingEntity entity) {
        boolean vanilla = state.isBed(level, position, entity);
        InteractionResult result = EntitySleepEvents.ALLOW_BED.invoker()
                .allowBed(entity, position, state, vanilla);
        return result == InteractionResult.PASS ? vanilla : result == InteractionResult.SUCCESS;
    }

    @Redirect(method = "startSleeping", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;setBedOccupied("
                    + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/entity/LivingEntity;Z)V"))
    private void loaderbridge$setBedOccupiedOnStart(BlockState state, Level level,
            BlockPos position, LivingEntity entity, boolean occupied) {
        if (!EntitySleepEvents.SET_BED_OCCUPATION_STATE.invoker()
                .setBedOccupationState(entity, position, state, occupied)) {
            state.setBedOccupied(level, position, entity, occupied);
        }
    }

    @Inject(method = "stopSleeping", at = @At("HEAD"))
    private void loaderbridge$stopSleeping(CallbackInfo callback) {
        getSleepingPos().ifPresent(position -> EntitySleepEvents.STOP_SLEEPING.invoker()
                .onStopSleeping((LivingEntity) (Object) this, position));
    }

    @Redirect(method = "lambda$stopSleeping$11", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;setBedOccupied("
                    + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/entity/LivingEntity;Z)V"))
    private void loaderbridge$setBedOccupiedOnStop(BlockState state, Level level,
            BlockPos position, LivingEntity entity, boolean occupied) {
        if (!EntitySleepEvents.SET_BED_OCCUPATION_STATE.invoker()
                .setBedOccupationState(entity, position, state, occupied)) {
            state.setBedOccupied(level, position, entity, occupied);
        }
    }

    @Redirect(method = "lambda$stopSleeping$11", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BedBlock;findStandUpPosition("
                    + "Lnet/minecraft/world/entity/EntityType;"
                    + "Lnet/minecraft/world/level/CollisionGetter;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;F)"
                    + "Ljava/util/Optional;"))
    private Optional<Vec3> loaderbridge$modifyWakePosition(EntityType<?> type,
            CollisionGetter collision, BlockPos position, Direction direction, float yaw) {
        Optional<Vec3> original = BedBlock.findStandUpPosition(
                type, collision, position, direction, yaw);
        LivingEntity self = (LivingEntity) (Object) this;
        BlockState state = self.level().getBlockState(position);
        Vec3 modified = EntitySleepEvents.MODIFY_WAKE_UP_POSITION.invoker()
                .modifyWakeUpPosition(self, position, state, original.orElse(null));
        return Optional.ofNullable(modified);
    }

    @Inject(method = "getBedOrientation", at = @At("RETURN"), cancellable = true)
    private void loaderbridge$modifySleepingDirection(
            CallbackInfoReturnable<Direction> callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        getSleepingPos().ifPresent(position -> callback.setReturnValue(
                EntitySleepEvents.MODIFY_SLEEPING_DIRECTION.invoker()
                        .modifySleepDirection(self, position, callback.getReturnValue())));
    }

    @Inject(method = "updateFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;"
                    + "getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)"
                    + "Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
    private void loaderbridge$updateFallFlying(CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!EntityElytraEvents.ALLOW.invoker().allowElytraFlight(self)) {
            if (!self.level().isClientSide) {
                ((EntitySharedFlagInvoker) self).loaderbridge$setSharedFlag(7, false);
            }
            callback.cancel();
        } else if (EntityElytraEvents.CUSTOM.invoker().useCustomElytra(self, true)) {
            callback.cancel();
        }
    }
}
