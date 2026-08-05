package net.fabricmc.fabric.api.event.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

@FunctionalInterface
public interface UseBlockCallback {
    Event<UseBlockCallback> EVENT = EventFactory.createArrayBacked(
            UseBlockCallback.class, listeners -> (player, world, hand, hit) -> {
                for (UseBlockCallback listener : listeners) {
                    InteractionResult result = listener.interact(player, world, hand, hit);
                    if (result != InteractionResult.PASS) return result;
                }
                return InteractionResult.PASS;
            });

    InteractionResult interact(Player player, Level world, InteractionHand hand,
            BlockHitResult hitResult);
}
