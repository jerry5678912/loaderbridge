package net.fabricmc.fabric.api.gamerule.v1;

import dev.loaderbridge.fabric.api.gamerule.CustomCategoryRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.world.level.GameRules;

public final class GameRuleRegistry {
    private GameRuleRegistry() { }
    public static <T extends GameRules.Value<T>> GameRules.Key<T> register(String name,
            GameRules.Category category, GameRules.Type<T> type) {
        return registerNative(name, category, type);
    }
    public static <T extends GameRules.Value<T>> GameRules.Key<T> register(String name,
            CustomGameRuleCategory category, GameRules.Type<T> type) {
        GameRules.Key<T> key = registerNative(name, GameRules.Category.MISC, type);
        CustomCategoryRegistry.put(key, category);
        return key;
    }
    public static boolean hasRegistration(String name) {
        AtomicBoolean found = new AtomicBoolean();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override public <T extends GameRules.Value<T>> void visit(
                    GameRules.Key<T> key, GameRules.Type<T> type) {
                if (key.getId().equals(name)) found.set(true);
            }
        });
        return found.get();
    }

    @SuppressWarnings("unchecked")
    private static <T extends GameRules.Value<T>> GameRules.Key<T> registerNative(String name,
            GameRules.Category category, GameRules.Type<T> type) {
        try {
            Method register = GameRules.class.getDeclaredMethod("register", String.class,
                    GameRules.Category.class, GameRules.Type.class);
            register.setAccessible(true);
            return (GameRules.Key<T>) register.invoke(null, name, category, type);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("LB-FAPI-GAMERULE-002: cannot access game-rule registry", exception);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("LB-FAPI-GAMERULE-002: game-rule registration failed",
                    exception.getCause());
        }
    }
}
