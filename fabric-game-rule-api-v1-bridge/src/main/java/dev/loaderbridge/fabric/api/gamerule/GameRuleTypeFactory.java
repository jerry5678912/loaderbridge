package dev.loaderbridge.fabric.api.gamerule;

import com.mojang.brigadier.arguments.ArgumentType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public final class GameRuleTypeFactory {
    private GameRuleTypeFactory() { }

    @SuppressWarnings("unchecked")
    public static <T extends GameRules.Value<T>> GameRules.Type<T> create(
            Supplier<ArgumentType<?>> argument, Function<GameRules.Type<T>, T> constructor,
            BiConsumer<MinecraftServer, T> callback,
            TriConsumer<GameRules.GameRuleTypeVisitor, GameRules.Key<T>, GameRules.Type<T>> visitor) {
        try {
            Constructor<?> target = GameRules.Type.class.getDeclaredConstructors()[0];
            target.setAccessible(true);
            Class<?> visitorCaller = target.getParameterTypes()[3];
            Object caller = Proxy.newProxyInstance(visitorCaller.getClassLoader(),
                    new Class<?>[] {visitorCaller}, (proxy, method, args) -> {
                        if (args != null && args.length == 3) {
                            visitor.accept((GameRules.GameRuleTypeVisitor) args[0],
                                    (GameRules.Key<T>) args[1], (GameRules.Type<T>) args[2]);
                        }
                        return null;
                    });
            return (GameRules.Type<T>) target.newInstance(argument, constructor, callback, caller);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("LB-FAPI-GAMERULE-001: cannot construct GameRules.Type", exception);
        }
    }

    @FunctionalInterface
    public interface TriConsumer<A, B, C> { void accept(A first, B second, C third); }
}
