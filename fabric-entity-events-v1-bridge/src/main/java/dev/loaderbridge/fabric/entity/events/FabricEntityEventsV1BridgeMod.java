package dev.loaderbridge.fabric.entity.events;

import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("loaderbridge_fabric_entity_events_v1")
public final class FabricEntityEventsV1BridgeMod {
    private static final Set<Mob> MIXIN_DISPATCHED_CONVERSIONS = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));
    private final Map<UUID, RespawnPair> pendingRespawns = new ConcurrentHashMap<>();

    public FabricEntityEventsV1BridgeMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(this::onMobConversion);
        MinecraftForge.EVENT_BUS.addListener(this::onClone);
        MinecraftForge.EVENT_BUS.addListener(this::onRespawn);
        MinecraftForge.EVENT_BUS.addListener(this::onJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onLeave);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerDimensionChange);
        MinecraftForge.EVENT_BUS.addListener(this::onSleepingLocationCheck);
        MinecraftForge.EVENT_BUS.addListener(this::onSleepingTimeCheck);
    }

    private void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(
                event.getEntity(), event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
    }

    private void onMobConversion(LivingConversionEvent.Post event) {
        if (event.getEntity() instanceof Mob previous && event.getOutcome() instanceof Mob converted) {
            if (MIXIN_DISPATCHED_CONVERSIONS.remove(converted)) return;
            ServerLivingEntityEvents.MOB_CONVERSION.invoker()
                    .onConversion(previous, converted, false);
        }
    }

    public static void onMixinMobConversion(Mob previous, Mob converted,
            boolean keepEquipment) {
        MIXIN_DISPATCHED_CONVERSIONS.add(converted);
        ServerLivingEntityEvents.MOB_CONVERSION.invoker()
                .onConversion(previous, converted, keepEquipment);
    }

    private void onClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)
                || !(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        boolean alive = !event.isWasDeath();
        ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(oldPlayer, newPlayer, alive);
        pendingRespawns.put(newPlayer.getUUID(), new RespawnPair(oldPlayer, newPlayer, alive));
    }

    private void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        RespawnPair pair = pendingRespawns.remove(player.getUUID());
        if (pair != null) {
            ServerPlayerEvents.AFTER_RESPAWN.invoker()
                    .afterRespawn(pair.oldPlayer(), pair.newPlayer(), pair.alive());
        }
    }

    private void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayerEvents.JOIN.invoker().onJoin(player);
        }
    }

    private void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayerEvents.LEAVE.invoker().onLeave(player);
        }
    }

    private void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel origin = player.server.getLevel(event.getFrom());
        ServerLevel destination = player.server.getLevel(event.getTo());
        if (origin != null && destination != null) {
            ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.invoker()
                    .afterChangeWorld(player, origin, destination);
        }
    }

    private void onSleepingLocationCheck(SleepingLocationCheckEvent event) {
        var entity = event.getEntity();
        var position = event.getSleepingLocation();
        var state = entity.level().getBlockState(position);
        boolean vanilla = state.isBed(entity.level(), position, entity);
        InteractionResult result = net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
                .ALLOW_BED.invoker().allowBed(entity, position, state, vanilla);
        applyResult(event, result);
    }

    private void onSleepingTimeCheck(SleepingTimeCheckEvent event) {
        var position = event.getSleepingLocation().orElseGet(event.getEntity()::blockPosition);
        boolean vanilla = !event.getEntity().level().isDay();
        InteractionResult result = net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
                .ALLOW_SLEEP_TIME.invoker()
                .allowSleepTime(event.getEntity(), position, vanilla);
        applyResult(event, result);
    }

    private static void applyResult(net.minecraftforge.eventbus.api.Event event,
            InteractionResult result) {
        if (result == InteractionResult.SUCCESS) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
        } else if (result == InteractionResult.FAIL) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    private record RespawnPair(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) { }
}
