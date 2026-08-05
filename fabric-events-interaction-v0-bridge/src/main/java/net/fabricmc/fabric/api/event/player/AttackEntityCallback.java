package net.fabricmc.fabric.api.event.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface AttackEntityCallback {
    Event<AttackEntityCallback> EVENT = EventFactory.createArrayBacked(
            AttackEntityCallback.class, listeners -> (player, world, hand, entity, hit) -> {
                for (AttackEntityCallback listener : listeners) {
                    InteractionResult result = listener.interact(player, world, hand, entity, hit);
                    if (result != InteractionResult.PASS) return result;
                }
                return InteractionResult.PASS;
            });

    InteractionResult interact(Player player, Level world, InteractionHand hand,
            Entity entity, @Nullable EntityHitResult hitResult);
}
