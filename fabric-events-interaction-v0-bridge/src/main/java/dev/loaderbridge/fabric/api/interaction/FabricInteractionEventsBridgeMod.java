package dev.loaderbridge.fabric.api.interaction;

import net.fabricmc.fabric.api.block.BlockAttackInteractionAware;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod("loaderbridge_fabric_events_interaction_v0")
public final class FabricInteractionEventsBridgeMod {
    @SuppressWarnings("deprecation")
    public FabricInteractionEventsBridgeMod() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            var state = world.getBlockState(pos);
            if (state instanceof BlockAttackInteractionAware aware
                    && aware.onAttackInteraction(state, world, pos, player, hand, direction)) {
                return InteractionResult.SUCCESS;
            }
            if (state.getBlock() instanceof BlockAttackInteractionAware aware
                    && aware.onAttackInteraction(state, world, pos, player, hand, direction)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.CANCELED.register((world, player, pos, state, entity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            BlockPos corner = pos.offset(-1, -1, -1);
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    for (int z = 0; z < 3; z++) {
                        serverPlayer.connection.send(new ClientboundBlockUpdatePacket(
                                world, corner.offset(x, y, z)));
                    }
                }
            }
        });

        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::attackBlock);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::attackEntity);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::useBlock);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::useItem);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::useEntity);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::useEntitySpecific);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::beforeBlockBreak);
    }

    private void attackBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        InteractionResult result = AttackBlockCallback.EVENT.invoker().interact(
                event.getEntity(), event.getLevel(), InteractionHand.MAIN_HAND,
                event.getPos(), event.getFace());
        cancelOnResult(event, result);
    }

    private void attackEntity(AttackEntityEvent event) {
        InteractionResult result = AttackEntityCallback.EVENT.invoker().interact(
                event.getEntity(), event.getEntity().level(), InteractionHand.MAIN_HAND,
                event.getTarget(), null);
        if (result != InteractionResult.PASS) event.setCanceled(true);
    }

    private void useBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = UseBlockCallback.EVENT.invoker().interact(
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        cancelOnResult(event, result);
    }

    private void useItem(PlayerInteractEvent.RightClickItem event) {
        var result = UseItemCallback.EVENT.invoker().interact(
                event.getEntity(), event.getLevel(), event.getHand());
        cancelOnResult(event, result.getResult());
    }

    private void useEntity(PlayerInteractEvent.EntityInteract event) {
        InteractionResult result = UseEntityCallback.EVENT.invoker().interact(
                event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), null);
        cancelOnResult(event, result);
    }

    private void useEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        EntityHitResult hit = new EntityHitResult(event.getTarget(),
                event.getTarget().position().add(event.getLocalPos()));
        InteractionResult result = UseEntityCallback.EVENT.invoker().interact(
                event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), hit);
        cancelOnResult(event, result);
    }

    private void beforeBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.world.level.Level level)) return;
        var entity = level.getBlockEntity(event.getPos());
        boolean allowed = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(
                level, event.getPlayer(), event.getPos(), event.getState(), entity);
        if (!allowed) {
            event.setCanceled(true);
            PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(
                    level, event.getPlayer(), event.getPos(), event.getState(), entity);
            BridgeInteractionEvents.clearPendingBreak();
        } else {
            BridgeInteractionEvents.setPendingBreak(
                    level, event.getPlayer(), event.getPos(), event.getState(), entity);
        }
    }

    private static void cancelOnResult(PlayerInteractEvent event, InteractionResult result) {
        if (result == InteractionResult.PASS) return;
        event.setCancellationResult(result);
        event.setCanceled(true);
    }
}
