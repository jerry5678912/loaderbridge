package dev.loaderbridge.fixture.entityevents;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class FabricEntityEventsFixture implements ModInitializer {
    private static final AtomicInteger ALLOW_DAMAGE = new AtomicInteger();
    private static final AtomicInteger AFTER_DAMAGE = new AtomicInteger();
    private static final AtomicInteger ALLOW_DEATH = new AtomicInteger();
    private static final AtomicInteger AFTER_DEATH = new AtomicInteger();
    private static final AtomicInteger COMBAT = new AtomicInteger();
    private static final AtomicInteger CONVERSION = new AtomicInteger();
    private static final AtomicInteger JOINS = new AtomicInteger();
    private static final AtomicInteger LEAVES = new AtomicInteger();
    private static final AtomicInteger START_SLEEPING = new AtomicInteger();
    private static final AtomicInteger STOP_SLEEPING = new AtomicInteger();
    private static final AtomicInteger SLEEP_SCENARIOS = new AtomicInteger();
    private static final AtomicInteger ALLOW_SLEEPING = new AtomicInteger();
    private static final AtomicInteger ALLOW_BED = new AtomicInteger();
    private static final AtomicInteger ALLOW_SLEEP_TIME = new AtomicInteger();
    private static final AtomicInteger ALLOW_NEARBY_MONSTERS = new AtomicInteger();
    private static final AtomicInteger ALLOW_RESETTING_TIME = new AtomicInteger();
    private static final AtomicInteger ALLOW_SETTING_SPAWN = new AtomicInteger();
    private static final AtomicInteger BED_OCCUPATION = new AtomicInteger();
    private static final AtomicInteger SLEEP_DIRECTION = new AtomicInteger();
    private static final AtomicInteger WAKE_POSITION = new AtomicInteger();
    private static final AtomicInteger ELYTRA_ALLOW = new AtomicInteger();
    private static final AtomicInteger ELYTRA_CUSTOM_START = new AtomicInteger();
    private static final AtomicInteger ELYTRA_CUSTOM_TICK = new AtomicInteger();
    private static final AtomicInteger ENTITY_WORLD_CHANGE = new AtomicInteger();
    private static final AtomicInteger PLAYER_WORLD_CHANGE = new AtomicInteger();
    private static final AtomicInteger COPY_FROM = new AtomicInteger();
    private static final AtomicInteger AFTER_RESPAWN = new AtomicInteger();
    private static final AtomicBoolean SERVER_BEHAVIOR_COMPLETE = new AtomicBoolean();
    private static final AtomicReference<Mob> SAVED_TARGET = new AtomicReference<>();
    private static final AtomicReference<ServerPlayer> ELYTRA_TARGET = new AtomicReference<>();
    private static final AtomicReference<Entity> WORLD_ENTITY_TARGET = new AtomicReference<>();
    private static final AtomicReference<ServerPlayer> PLAYER_WORLD_TARGET = new AtomicReference<>();
    private static final AtomicReference<ServerPlayer> RESPAWN_OLD = new AtomicReference<>();
    private static final AtomicReference<ServerPlayer> RESPAWN_NEW = new AtomicReference<>();
    private static final AtomicReference<ElytraScenario> ELYTRA_SCENARIO = new AtomicReference<>();
    private static final AtomicReference<RuntimeException> PENDING_FAILURE = new AtomicReference<>();

    @Override public void onInitialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity == SAVED_TARGET.get()) ALLOW_DAMAGE.incrementAndGet();
            return true;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (entity == SAVED_TARGET.get()) {
                if (base <= 0 || taken <= 0 || blocked) {
                    throw new IllegalStateException("Entity fixture received invalid damage values: "
                            + base + "/" + taken + "/" + blocked);
                }
                AFTER_DAMAGE.incrementAndGet();
            }
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity != SAVED_TARGET.get()) return true;
            if (amount != 5.0F) {
                throw new IllegalStateException("Entity fixture lost fatal damage amount: " + amount);
            }
            ALLOW_DEATH.incrementAndGet();
            entity.setHealth(5.0F);
            return false;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> AFTER_DEATH.incrementAndGet());
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(
                (world, attacker, killed) -> COMBAT.incrementAndGet());
        ServerLivingEntityEvents.MOB_CONVERSION.register((previous, converted, keepEquipment) -> {
            if (previous.getType() == EntityType.ZOMBIE && converted.getType() == EntityType.DROWNED) {
                CONVERSION.incrementAndGet();
            }
        });
        EntityElytraEvents.ALLOW.register(entity -> {
            if (entity == ELYTRA_TARGET.get()) ELYTRA_ALLOW.incrementAndGet();
            return true;
        });
        EntityElytraEvents.CUSTOM.register((entity, tick) -> {
            if (entity != ELYTRA_TARGET.get()) return false;
            if (tick) ELYTRA_CUSTOM_TICK.incrementAndGet();
            else ELYTRA_CUSTOM_START.incrementAndGet();
            return true;
        });
        ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register(
                (original, replacement, origin, destination) -> {
                    if (original != WORLD_ENTITY_TARGET.get()) return;
                    if (original == replacement || origin == destination
                            || replacement.level() != destination) {
                        throw new IllegalStateException("Entity fixture received invalid entity "
                                + "world-change values");
                    }
                    ENTITY_WORLD_CHANGE.incrementAndGet();
                });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
                (player, origin, destination) -> {
                    if (player != PLAYER_WORLD_TARGET.get()) return;
                    if (origin == destination || player.serverLevel() != destination) {
                        throw new IllegalStateException("Entity fixture received invalid player "
                                + "world-change values");
                    }
                    PLAYER_WORLD_CHANGE.incrementAndGet();
                });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (oldPlayer != RESPAWN_OLD.get()) return;
            verifyRespawnPair(oldPlayer, newPlayer, alive, "copy");
            RESPAWN_NEW.set(newPlayer);
            COPY_FROM.incrementAndGet();
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (oldPlayer != RESPAWN_OLD.get()) return;
            verifyRespawnPair(oldPlayer, newPlayer, alive, "after respawn");
            if (newPlayer != RESPAWN_NEW.get()) {
                throw new IllegalStateException("Entity fixture respawn callbacks disagreed");
            }
            AFTER_RESPAWN.incrementAndGet();
        });
        EntitySleepEvents.ALLOW_SLEEPING.register((player, position) -> {
            ALLOW_SLEEPING.incrementAndGet();
            return null;
        });
        EntitySleepEvents.ALLOW_BED.register(
                (entity, position, state, vanilla) -> {
                    ALLOW_BED.incrementAndGet();
                    return InteractionResult.SUCCESS;
                });
        EntitySleepEvents.ALLOW_SLEEP_TIME.register(
                (player, position, vanilla) -> {
                    ALLOW_SLEEP_TIME.incrementAndGet();
                    return InteractionResult.SUCCESS;
                });
        EntitySleepEvents.ALLOW_NEARBY_MONSTERS.register(
                (player, position, vanilla) -> {
                    ALLOW_NEARBY_MONSTERS.incrementAndGet();
                    return InteractionResult.SUCCESS;
                });
        EntitySleepEvents.ALLOW_RESETTING_TIME.register(player -> {
            ALLOW_RESETTING_TIME.incrementAndGet();
            return false;
        });
        EntitySleepEvents.ALLOW_SETTING_SPAWN.register((player, position) -> {
            ALLOW_SETTING_SPAWN.incrementAndGet();
            return true;
        });
        EntitySleepEvents.SET_BED_OCCUPATION_STATE.register((entity, position, state, occupied) -> {
            BED_OCCUPATION.incrementAndGet();
            return false;
        });
        EntitySleepEvents.MODIFY_SLEEPING_DIRECTION.register(
                (entity, position, direction) -> {
                    SLEEP_DIRECTION.incrementAndGet();
                    return direction;
                });
        EntitySleepEvents.MODIFY_WAKE_UP_POSITION.register(
                (entity, position, state, wakePosition) -> {
                    WAKE_POSITION.incrementAndGet();
                    return wakePosition;
                });
        EntitySleepEvents.START_SLEEPING.register(
                (entity, position) -> START_SLEEPING.incrementAndGet());
        EntitySleepEvents.STOP_SLEEPING.register(
                (entity, position) -> STOP_SLEEPING.incrementAndGet());
        ServerPlayerEvents.JOIN.register(player -> {
            int joins = JOINS.incrementAndGet();
            player.server.execute(() -> {
                try {
                    verifyPlayerAndSleepHooks(player, joins);
                    beginElytraScenario(player, joins);
                } catch (RuntimeException failure) {
                    PENDING_FAILURE.compareAndSet(null, failure);
                }
            });
        });
        ServerPlayerEvents.LEAVE.register(player -> {
            int leaves = LEAVES.incrementAndGet();
            System.out.println("LOADERBRIDGE_ENTITY_PLAYER_LEAVE_READY leaves=" + leaves);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (SERVER_BEHAVIOR_COMPLETE.compareAndSet(false, true)) verifyRuntimeHooks(server);
        });
        ServerTickEvents.END_SERVER_TICK.register(FabricEntityEventsFixture::finishElytraScenario);
    }

    private static void beginElytraScenario(ServerPlayer player, int joins) {
        int allowBefore = ELYTRA_ALLOW.get();
        int startBefore = ELYTRA_CUSTOM_START.get();
        int tickBefore = ELYTRA_CUSTOM_TICK.get();
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        player.teleportTo(player.getX(), player.getY() + 5, player.getZ());
        player.setNoGravity(true);
        player.setOnGround(false);
        player.setDeltaMovement(0, -0.1, 0);
        ELYTRA_TARGET.set(player);
        ELYTRA_SCENARIO.set(new ElytraScenario(
                player, joins, allowBefore, startBefore, tickBefore));
        if (!player.tryToStartFallFlying() || !player.isFallFlying()
                || ELYTRA_ALLOW.get() <= allowBefore
                || ELYTRA_CUSTOM_START.get() != startBefore + 1) {
            throw new IllegalStateException("Entity fixture could not start custom elytra flight");
        }
    }

    private static void finishElytraScenario(net.minecraft.server.MinecraftServer server) {
        RuntimeException failure = PENDING_FAILURE.getAndSet(null);
        if (failure != null) throw failure;
        ElytraScenario scenario = ELYTRA_SCENARIO.get();
        if (scenario == null || ELYTRA_CUSTOM_TICK.get() <= scenario.tickBefore()) return;
        if (!ELYTRA_SCENARIO.compareAndSet(scenario, null)) return;
        ServerPlayer player = scenario.player();
        if (!player.isFallFlying() || ELYTRA_ALLOW.get() <= scenario.allowBefore()
                || ELYTRA_CUSTOM_START.get() != scenario.startBefore() + 1) {
            throw new IllegalStateException("Entity fixture custom elytra tick was incomplete: "
                    + "flying=" + player.isFallFlying()
                    + " allow=" + ELYTRA_ALLOW.get() + "/" + scenario.allowBefore()
                    + " start=" + ELYTRA_CUSTOM_START.get() + "/" + scenario.startBefore()
                    + " tick=" + ELYTRA_CUSTOM_TICK.get() + "/" + scenario.tickBefore());
        }
        player.stopFallFlying();
        player.setNoGravity(false);
        ELYTRA_TARGET.set(null);
        server.execute(() -> {
            try {
                verifyWorldChangeAndRespawn(player, scenario.joins());
            } catch (RuntimeException laterFailure) {
                PENDING_FAILURE.compareAndSet(null, laterFailure);
            }
        });
    }

    private static void verifyWorldChangeAndRespawn(ServerPlayer player, int joins) {
        ServerLevel overworld = player.serverLevel();
        ServerLevel nether = require(player.server.getLevel(Level.NETHER), "nether level");
        int entityWorldBefore = ENTITY_WORLD_CHANGE.get();
        Mob original = require(EntityType.ZOMBIE.create(overworld), "world-change entity");
        original.moveTo(overworld.getSharedSpawnPos(), 0, 0);
        overworld.addFreshEntity(original);
        WORLD_ENTITY_TARGET.set(original);
        Entity replacement = original.changeDimension(new DimensionTransition(
                nether, Vec3.atBottomCenterOf(nether.getSharedSpawnPos()), Vec3.ZERO,
                0, 0, DimensionTransition.DO_NOTHING));
        WORLD_ENTITY_TARGET.set(null);
        if (replacement == null || ENTITY_WORLD_CHANGE.get() != entityWorldBefore + 1) {
            throw new IllegalStateException("Entity fixture missed entity world change");
        }
        replacement.discard();

        int playerWorldBefore = PLAYER_WORLD_CHANGE.get();
        PLAYER_WORLD_TARGET.set(player);
        Entity netherPlayer = player.changeDimension(new DimensionTransition(
                nether, Vec3.atBottomCenterOf(nether.getSharedSpawnPos()), Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        if (netherPlayer != player) {
            throw new IllegalStateException("Entity fixture player replacement was unexpected");
        }
        Entity returnedPlayer = player.changeDimension(new DimensionTransition(
                overworld, Vec3.atBottomCenterOf(overworld.getSharedSpawnPos()), Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        PLAYER_WORLD_TARGET.set(null);
        if (returnedPlayer != player || PLAYER_WORLD_CHANGE.get() != playerWorldBefore + 2) {
            throw new IllegalStateException("Entity fixture missed player world changes");
        }

        int copyBefore = COPY_FROM.get();
        int respawnBefore = AFTER_RESPAWN.get();
        player.experienceLevel = 7;
        RESPAWN_OLD.set(player);
        RESPAWN_NEW.set(null);
        ServerPlayer newPlayer = player.server.getPlayerList().respawn(
                player, true, Entity.RemovalReason.CHANGED_DIMENSION);
        if (newPlayer == player || newPlayer.experienceLevel != 7
                || COPY_FROM.get() != copyBefore + 1
                || AFTER_RESPAWN.get() != respawnBefore + 1
                || RESPAWN_NEW.get() != newPlayer) {
            throw new IllegalStateException("Entity fixture respawn cycle was incomplete");
        }
        RESPAWN_OLD.set(null);
        RESPAWN_NEW.set(null);
        System.out.println("LOADERBRIDGE_ENTITY_DEEP_READY joins=" + joins
                + " elytraAllow=" + ELYTRA_ALLOW.get()
                + " customStart=" + ELYTRA_CUSTOM_START.get()
                + " customTick=" + ELYTRA_CUSTOM_TICK.get()
                + " entityWorld=" + ENTITY_WORLD_CHANGE.get()
                + " playerWorld=" + PLAYER_WORLD_CHANGE.get()
                + " copy=" + COPY_FROM.get() + " respawn=" + AFTER_RESPAWN.get());
    }

    private static void verifyRespawnPair(ServerPlayer oldPlayer, ServerPlayer newPlayer,
            boolean alive, String phase) {
        if (oldPlayer == newPlayer || !oldPlayer.getUUID().equals(newPlayer.getUUID()) || !alive) {
            throw new IllegalStateException("Entity fixture received invalid " + phase
                    + " values");
        }
    }

    private static void verifyPlayerAndSleepHooks(net.minecraft.server.level.ServerPlayer player,
            int joins) {
        var level = player.serverLevel();
        level.setDayTime(13000);
        BlockPos foot = player.blockPosition().offset(2, 0, 0);
        BlockPos head = foot.relative(Direction.NORTH);
        level.setBlockAndUpdate(foot.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(head.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(foot.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(head.above(), Blocks.AIR.defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlockAndUpdate(foot.relative(direction).above(),
                    Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(foot, Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(BedBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(head, Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.HEAD)
                .setValue(BedBlock.FACING, Direction.NORTH));

        int startsBefore = START_SLEEPING.get();
        int stopsBefore = STOP_SLEEPING.get();
        int[] callbacksBefore = sleepCallbackCounts();
        player.setGameMode(GameType.SURVIVAL);
        var result = player.startSleepInBed(foot);
        if (result.left().isPresent() || !player.isSleeping()) {
            throw new IllegalStateException("Entity fixture could not start player sleep: " + result);
        }
        for (int tick = 0; tick < 101; tick++) player.doTick();
        if (player.isSleepingLongEnough()) {
            throw new IllegalStateException("Entity fixture could not veto resetting time");
        }
        player.stopSleeping();
        player.setGameMode(GameType.CREATIVE);
        if (player.isSleeping() || START_SLEEPING.get() != startsBefore + 1
                || STOP_SLEEPING.get() != stopsBefore + 1) {
            throw new IllegalStateException("Entity fixture sleep callbacks were incomplete: "
                    + START_SLEEPING + "/" + STOP_SLEEPING);
        }
        int[] callbacksAfter = sleepCallbackCounts();
        for (int index = 0; index < callbacksBefore.length; index++) {
            if (callbacksAfter[index] <= callbacksBefore[index]) {
                throw new IllegalStateException("Entity fixture missed sleep callback index "
                        + index + ": " + java.util.Arrays.toString(callbacksAfter));
            }
        }
        int scenarios = SLEEP_SCENARIOS.incrementAndGet();
        System.out.println("LOADERBRIDGE_ENTITY_PLAYER_SLEEP_READY joins=" + joins
                + " leaves=" + LEAVES.get() + " scenarios=" + scenarios
                + " resetVeto=" + ALLOW_RESETTING_TIME.get());
    }

    private static int[] sleepCallbackCounts() {
        return new int[] {ALLOW_SLEEPING.get(), ALLOW_BED.get(), ALLOW_SLEEP_TIME.get(),
                ALLOW_NEARBY_MONSTERS.get(), ALLOW_RESETTING_TIME.get(),
                ALLOW_SETTING_SPAWN.get(), BED_OCCUPATION.get(), SLEEP_DIRECTION.get(),
                WAKE_POSITION.get()};
    }

    private static void verifyRuntimeHooks(net.minecraft.server.MinecraftServer server) {
        var level = server.overworld();
        Mob target = require(EntityType.ZOMBIE.create(level), "damage target");
        target.moveTo(level.getSharedSpawnPos(), 0, 0);
        level.addFreshEntity(target);
        SAVED_TARGET.set(target);

        if (!target.hurt(level.damageSources().magic(), 2.0F)) {
            throw new IllegalStateException("Entity fixture nonfatal damage was rejected");
        }
        target.setHealth(1.0F);
        target.hurt(level.damageSources().magic(), 5.0F);
        if (target.isDeadOrDying() || target.getHealth() != 5.0F) {
            throw new IllegalStateException("Entity fixture fatal-damage cancellation failed");
        }

        Mob attacker = require(EntityType.ZOMBIE.create(level), "combat attacker");
        Mob victim = require(EntityType.ZOMBIE.create(level), "combat victim");
        attacker.moveTo(level.getSharedSpawnPos(), 0, 0);
        victim.moveTo(level.getSharedSpawnPos(), 0, 0);
        level.addFreshEntity(attacker);
        level.addFreshEntity(victim);
        victim.setHealth(1.0F);
        victim.hurt(level.damageSources().mobAttack(attacker), 5.0F);

        Mob converting = require(EntityType.ZOMBIE.create(level), "conversion source");
        converting.moveTo(level.getSharedSpawnPos(), 0, 0);
        level.addFreshEntity(converting);
        Mob converted = converting.convertTo(EntityType.DROWNED, false);
        if (converted == null) throw new IllegalStateException("Entity fixture conversion failed");

        SAVED_TARGET.set(null);
        target.discard();
        attacker.discard();
        victim.discard();
        converted.discard();

        if (ALLOW_DAMAGE.get() != 2 || AFTER_DAMAGE.get() != 2
                || ALLOW_DEATH.get() != 1 || AFTER_DEATH.get() < 1
                || COMBAT.get() < 1 || CONVERSION.get() != 1) {
            throw new IllegalStateException("Entity fixture event counts were incomplete: "
                    + ALLOW_DAMAGE + "/" + AFTER_DAMAGE + "/" + ALLOW_DEATH + "/"
                    + AFTER_DEATH + "/" + COMBAT + "/" + CONVERSION);
        }
        System.out.println("LOADERBRIDGE_ENTITY_EVENTS_READY allowDamage=2 afterDamage=2 "
                + "allowDeath=1 afterDeath=" + AFTER_DEATH.get()
                + " combat=" + COMBAT.get() + " conversion=1");
    }

    private static <T> T require(T value, String label) {
        if (value == null) throw new IllegalStateException("Could not create " + label);
        return value;
    }

    private record ElytraScenario(ServerPlayer player, int joins,
            int allowBefore, int startBefore, int tickBefore) { }
}
