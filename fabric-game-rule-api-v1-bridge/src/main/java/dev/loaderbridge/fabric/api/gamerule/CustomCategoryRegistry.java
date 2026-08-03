package dev.loaderbridge.fabric.api.gamerule;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
import net.minecraft.world.level.GameRules;

public final class CustomCategoryRegistry {
    private static final Map<GameRules.Key<?>, CustomGameRuleCategory> VALUES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private CustomCategoryRegistry() { }
    public static void put(GameRules.Key<?> key, CustomGameRuleCategory category) { VALUES.put(key, category); }
    public static Optional<CustomGameRuleCategory> get(GameRules.Key<?> key) {
        return Optional.ofNullable(VALUES.get(key));
    }
}
