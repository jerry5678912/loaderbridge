package dev.loaderbridge.fixture.entityevents;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.GameType;

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
    private static final AtomicInteger ALLOW_SETTING_SPAWN = new AtomicInteger();
    private static final AtomicInteger BED_OCCUPATION = new AtomicInteger();
    private static final AtomicInteger SLEEP_DIRECTION = new AtomicInteger();
    private static final AtomicInteger WAKE_POSITION = new AtomicInteger();
    private static final AtomicBoolean SERVER_BEHAVIOR_COMPLETE = new AtomicBoolean();
    private static final AtomicReference<Mob> SAVED_TARGET = new AtomicReference<>();

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
        EntitySleepEvents.ALLOW_RESETTING_TIME.register(player -> true);
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
            player.server.execute(() -> verifyPlayerAndSleepHooks(player, joins));
        });
        ServerPlayerEvents.LEAVE.register(player -> {
            int leaves = LEAVES.incrementAndGet();
            System.out.println("LOADERBRIDGE_ENTITY_PLAYER_LEAVE_READY leaves=" + leaves);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (SERVER_BEHAVIOR_COMPLETE.compareAndSet(false, true)) verifyRuntimeHooks(server);
        });
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
                + " leaves=" + LEAVES.get() + " scenarios=" + scenarios);
    }

    private static int[] sleepCallbackCounts() {
        return new int[] {ALLOW_SLEEPING.get(), ALLOW_BED.get(), ALLOW_SLEEP_TIME.get(),
                ALLOW_NEARBY_MONSTERS.get(), ALLOW_SETTING_SPAWN.get(), BED_OCCUPATION.get(),
                SLEEP_DIRECTION.get(), WAKE_POSITION.get()};
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
}
