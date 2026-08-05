package dev.loaderbridge.fixture.interaction;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Deep runtime exercise of the server/shared interaction callback surface. */
public final class FabricInteractionEventsFixture implements ModInitializer {
    private static final AtomicBoolean COMPLETE = new AtomicBoolean();
    private static final AtomicReference<Scenario> SCENARIO = new AtomicReference<>();
    private static final AtomicInteger ATTACK_BLOCK = new AtomicInteger();
    private static final AtomicInteger ATTACK_ENTITY = new AtomicInteger();
    private static final AtomicInteger USE_BLOCK = new AtomicInteger();
    private static final AtomicInteger USE_ENTITY = new AtomicInteger();
    private static final AtomicInteger USE_ITEM = new AtomicInteger();
    private static final AtomicInteger BREAK_BEFORE = new AtomicInteger();
    private static final AtomicInteger BREAK_CANCELED = new AtomicInteger();
    private static final AtomicInteger BREAK_AFTER = new AtomicInteger();

    @Override
    public void onInitialize() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            Scenario scenario = SCENARIO.get();
            if (scenario != null && pos.equals(scenario.attackBlock())) {
                ATTACK_BLOCK.incrementAndGet();
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            Scenario scenario = SCENARIO.get();
            if (scenario != null && entity == scenario.entity()) {
                ATTACK_ENTITY.incrementAndGet();
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            Scenario scenario = SCENARIO.get();
            if (scenario != null && hit.getBlockPos().equals(scenario.useBlock())) {
                USE_BLOCK.incrementAndGet();
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            Scenario scenario = SCENARIO.get();
            if (scenario != null && entity == scenario.entity()) {
                USE_ENTITY.incrementAndGet();
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            Scenario scenario = SCENARIO.get();
            if (scenario != null && player == scenario.player()) {
                USE_ITEM.incrementAndGet();
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            return InteractionResultHolder.pass(ItemStack.EMPTY);
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            Scenario scenario = SCENARIO.get();
            if (scenario == null) return true;
            if (pos.equals(scenario.cancelBreak())) {
                BREAK_BEFORE.incrementAndGet();
                return false;
            }
            if (pos.equals(scenario.successBreak())) BREAK_BEFORE.incrementAndGet();
            return true;
        });
        PlayerBlockBreakEvents.CANCELED.register((world, player, pos, state, entity) -> {
            Scenario scenario = SCENARIO.get();
            if (scenario != null && pos.equals(scenario.cancelBreak()) && state.is(Blocks.STONE)) {
                BREAK_CANCELED.incrementAndGet();
            }
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            Scenario scenario = SCENARIO.get();
            if (scenario != null && pos.equals(scenario.successBreak()) && state.is(Blocks.STONE)) {
                BREAK_AFTER.incrementAndGet();
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                System.out.println("LOADERBRIDGE_FABRIC_INTERACTION_EVENTS_LOADED"));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (COMPLETE.get() || server.getPlayerList().getPlayers().isEmpty()) return;
            runScenario(server.getPlayerList().getPlayers().getFirst());
        });
    }

    private static void runScenario(ServerPlayer player) {
        if (!COMPLETE.compareAndSet(false, true)) return;
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition().offset(3, 0, 0);
        BlockPos attack = origin;
        BlockPos cancelBreak = origin.offset(1, 0, 0);
        BlockPos successBreak = origin.offset(2, 0, 0);
        BlockPos useBlock = origin.offset(3, 0, 0);
        level.setBlockAndUpdate(attack, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(cancelBreak, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(successBreak, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(useBlock, Blocks.STONE.defaultBlockState());

        ArmorStand entity = EntityType.ARMOR_STAND.create(level);
        if (entity == null) throw new IllegalStateException("could not create interaction target");
        entity.moveTo(Vec3.atCenterOf(origin.offset(0, 1, 1)));
        if (!level.addFreshEntity(entity)) {
            throw new IllegalStateException("could not spawn interaction target");
        }
        SCENARIO.set(new Scenario(player, attack, cancelBreak, successBreak, useBlock, entity));

        player.gameMode.handleBlockBreakAction(attack,
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.UP, level.getMaxBuildHeight(), 1);
        boolean canceledBreak = player.gameMode.destroyBlock(cancelBreak);
        boolean successfulBreak = player.gameMode.destroyBlock(successBreak);

        ItemStack previous = player.getMainHandItem().copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        InteractionResult useBlockResult = player.gameMode.useItemOn(
                player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(useBlock), Direction.UP, useBlock, false));
        InteractionResult useItemResult = player.gameMode.useItem(
                player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
        player.attack(entity);
        InteractionResult useEntityResult = player.interactOn(entity, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, previous);

        if (ATTACK_BLOCK.get() != 1 || !level.getBlockState(attack).is(Blocks.STONE)
                || canceledBreak || !level.getBlockState(cancelBreak).is(Blocks.STONE)
                || !successfulBreak || !level.getBlockState(successBreak).isAir()
                || useBlockResult != InteractionResult.FAIL
                || useItemResult != InteractionResult.FAIL
                || useEntityResult != InteractionResult.FAIL
                || ATTACK_ENTITY.get() != 1 || USE_BLOCK.get() != 1
                || USE_ITEM.get() != 1 || USE_ENTITY.get() != 1
                || BREAK_BEFORE.get() != 2 || BREAK_CANCELED.get() != 1
                || BREAK_AFTER.get() != 1 || !entity.isAlive()) {
            throw new IllegalStateException("interaction callback scenario failed: "
                    + counters(canceledBreak, successfulBreak, useBlockResult,
                            useItemResult, useEntityResult));
        }
        System.out.println("LOADERBRIDGE_FABRIC_INTERACTION_EVENTS_READY "
                + counters(canceledBreak, successfulBreak, useBlockResult,
                        useItemResult, useEntityResult));
    }

    private static String counters(boolean canceledBreak, boolean successfulBreak,
            InteractionResult useBlockResult, InteractionResult useItemResult,
            InteractionResult useEntityResult) {
        return "attackBlock=" + ATTACK_BLOCK + ",attackEntity=" + ATTACK_ENTITY
                + ",useBlock=" + USE_BLOCK + ",useItem=" + USE_ITEM
                + ",useEntity=" + USE_ENTITY + ",before=" + BREAK_BEFORE
                + ",canceled=" + BREAK_CANCELED + ",after=" + BREAK_AFTER
                + ",cancelResult=" + canceledBreak + ",breakResult=" + successfulBreak
                + ",results=" + useBlockResult + "/" + useItemResult + "/" + useEntityResult;
    }

    private record Scenario(ServerPlayer player, BlockPos attackBlock, BlockPos cancelBreak,
            BlockPos successBreak, BlockPos useBlock, Entity entity) {
    }
}
