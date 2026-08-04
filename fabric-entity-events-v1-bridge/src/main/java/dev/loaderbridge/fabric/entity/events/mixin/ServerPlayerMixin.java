package dev.loaderbridge.fabric.entity.events.mixin;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "die", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;getKillCredit()"
                    + "Lnet/minecraft/world/entity/LivingEntity;"))
    private void loaderbridge$afterPlayerKilled(DamageSource source, CallbackInfo callback) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        Entity attacker = source.getEntity();
        if (attacker != null) {
            ServerLevel level = self.serverLevel();
            attacker.killedEntity(level, self);
            ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.invoker()
                    .afterKilledOtherEntity(level, attacker, self);
        }
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void loaderbridge$afterPlayerDeath(DamageSource source, CallbackInfo callback) {
        ServerLivingEntityEvents.AFTER_DEATH.invoker()
                .afterDeath((ServerPlayer) (Object) this, source);
    }

    @Inject(method = "startSleepInBed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;startSleepInBed("
                    + "Lnet/minecraft/core/BlockPos;)Lcom/mojang/datafixers/util/Either;"),
            cancellable = true)
    private void loaderbridge$allowSleeping(BlockPos position,
            CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> callback) {
        Player.BedSleepingProblem failure = EntitySleepEvents.ALLOW_SLEEPING.invoker()
                .allowSleep((ServerPlayer) (Object) this, position);
        if (failure != null) callback.setReturnValue(Either.left(failure));
    }

    @Redirect(method = "startSleepInBed", at = @At(value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z"))
    private boolean loaderbridge$allowNearbyMonsters(List<?> monsters, BlockPos position) {
        boolean vanilla = monsters.isEmpty();
        InteractionResult result = EntitySleepEvents.ALLOW_NEARBY_MONSTERS.invoker()
                .allowNearbyMonsters((ServerPlayer) (Object) this, position, vanilla);
        return result == InteractionResult.PASS ? vanilla : result == InteractionResult.SUCCESS;
    }

    @Redirect(method = "startSleepInBed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition("
                    + "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;"
                    + "FZZ)V"))
    private void loaderbridge$allowSettingSpawn(ServerPlayer player,
            ResourceKey<Level> dimension, BlockPos position, float angle,
            boolean forced, boolean sendMessage) {
        if (EntitySleepEvents.ALLOW_SETTING_SPAWN.invoker()
                .allowSettingSpawn(player, position)) {
            player.setRespawnPosition(dimension, position, angle, forced, sendMessage);
        }
    }

    @Redirect(method = "startSleepInBed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getValue("
                    + "Lnet/minecraft/world/level/block/state/properties/Property;)"
                    + "Ljava/lang/Comparable;", ordinal = 0))
    private Comparable<?> loaderbridge$modifyInitialSleepDirection(BlockState state,
            Property<?> property, BlockPos position) {
        Direction vanilla = state.hasProperty(property)
                ? (Direction) state.getValue(property) : null;
        return EntitySleepEvents.MODIFY_SLEEPING_DIRECTION.invoker()
                .modifySleepDirection((ServerPlayer) (Object) this, position, vanilla);
    }
}
