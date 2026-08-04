package net.fabricmc.fabric.api.entity.event.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public final class ServerPlayerEvents {
    public static final Event<CopyFrom> COPY_FROM = event(CopyFrom.class,
            listeners -> (oldPlayer, newPlayer, alive) -> {
                for (CopyFrom listener : listeners) listener.copyFromPlayer(oldPlayer, newPlayer, alive);
            });
    public static final Event<AfterRespawn> AFTER_RESPAWN = event(AfterRespawn.class,
            listeners -> (oldPlayer, newPlayer, alive) -> {
                for (AfterRespawn listener : listeners) listener.afterRespawn(oldPlayer, newPlayer, alive);
            });
    public static final Event<Join> JOIN = event(Join.class, listeners -> player -> {
        for (Join listener : listeners) listener.onJoin(player);
    });
    public static final Event<Leave> LEAVE = event(Leave.class, listeners -> player -> {
        for (Leave listener : listeners) listener.onLeave(player);
    });
    @Deprecated
    public static final Event<AllowDeath> ALLOW_DEATH = event(AllowDeath.class,
            listeners -> (player, source, amount) -> {
                for (AllowDeath listener : listeners) {
                    if (!listener.allowDeath(player, source, amount)) return false;
                }
                return true;
            });

    static {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                !(entity instanceof ServerPlayer player)
                        || ALLOW_DEATH.invoker().allowDeath(player, source, amount));
    }

    private static <T> Event<T> event(Class<T> type,
            java.util.function.Function<T[], T> factory) {
        return EventFactory.createArrayBacked(type, factory);
    }

    @FunctionalInterface public interface CopyFrom {
        void copyFromPlayer(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive);
    }
    @FunctionalInterface public interface AfterRespawn {
        void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive);
    }
    @FunctionalInterface public interface Join { void onJoin(ServerPlayer player); }
    @FunctionalInterface public interface Leave { void onLeave(ServerPlayer player); }
    @Deprecated @FunctionalInterface public interface AllowDeath {
        boolean allowDeath(ServerPlayer player, DamageSource source, float amount);
    }

    private ServerPlayerEvents() { }
}
