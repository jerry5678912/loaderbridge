package net.fabricmc.fabric.api.gamerule.v1;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.loaderbridge.fabric.api.gamerule.GameRuleTypeFactory;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;
import net.fabricmc.fabric.api.gamerule.v1.rule.EnumRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

@SuppressWarnings("deprecation")
public final class GameRuleFactory {
    private GameRuleFactory() { }
    public static GameRules.Type<GameRules.BooleanValue> createBooleanRule(boolean value) {
        return createBooleanRule(value, (server, rule) -> { });
    }
    public static GameRules.Type<GameRules.BooleanValue> createBooleanRule(boolean value,
            BiConsumer<MinecraftServer, GameRules.BooleanValue> callback) {
        return GameRuleTypeFactory.create(BoolArgumentType::bool,
                type -> new GameRules.BooleanValue(type, value), callback,
                GameRules.GameRuleTypeVisitor::visitBoolean);
    }
    public static GameRules.Type<GameRules.IntegerValue> createIntRule(int value) {
        return createIntRule(value, Integer.MIN_VALUE, Integer.MAX_VALUE, (server, rule) -> { });
    }
    public static GameRules.Type<GameRules.IntegerValue> createIntRule(int value,
            BiConsumer<MinecraftServer, GameRules.IntegerValue> callback) {
        return createIntRule(value, Integer.MIN_VALUE, Integer.MAX_VALUE, callback);
    }
    public static GameRules.Type<GameRules.IntegerValue> createIntRule(int value, int minimum) {
        return createIntRule(value, minimum, Integer.MAX_VALUE, (server, rule) -> { });
    }
    public static GameRules.Type<GameRules.IntegerValue> createIntRule(int value, int minimum,
            BiConsumer<MinecraftServer, GameRules.IntegerValue> callback) {
        return createIntRule(value, minimum, Integer.MAX_VALUE, callback);
    }
    public static GameRules.Type<GameRules.IntegerValue> createIntRule(int value, int minimum, int maximum) {
        return createIntRule(value, minimum, maximum, (server, rule) -> { });
    }
    public static GameRules.Type<GameRules.IntegerValue> createIntRule(int value, int minimum, int maximum,
            BiConsumer<MinecraftServer, GameRules.IntegerValue> callback) {
        return GameRuleTypeFactory.create(() -> IntegerArgumentType.integer(minimum, maximum),
                type -> new GameRules.IntegerValue(type, value), callback,
                GameRules.GameRuleTypeVisitor::visitInteger);
    }
    public static GameRules.Type<DoubleRule> createDoubleRule(double value) {
        return createDoubleRule(value, Double.MIN_VALUE, Double.MAX_VALUE, (server, rule) -> { });
    }
    public static GameRules.Type<DoubleRule> createDoubleRule(double value,
            BiConsumer<MinecraftServer, DoubleRule> callback) {
        return createDoubleRule(value, Double.MIN_VALUE, Double.MAX_VALUE, callback);
    }
    public static GameRules.Type<DoubleRule> createDoubleRule(double value, double minimum) {
        return createDoubleRule(value, minimum, Double.MAX_VALUE, (server, rule) -> { });
    }
    public static GameRules.Type<DoubleRule> createDoubleRule(double value, double minimum,
            BiConsumer<MinecraftServer, DoubleRule> callback) {
        return createDoubleRule(value, minimum, Double.MAX_VALUE, callback);
    }
    public static GameRules.Type<DoubleRule> createDoubleRule(double value, double minimum, double maximum) {
        return createDoubleRule(value, minimum, maximum, (server, rule) -> { });
    }
    public static GameRules.Type<DoubleRule> createDoubleRule(double value, double minimum, double maximum,
            BiConsumer<MinecraftServer, DoubleRule> callback) {
        return GameRuleTypeFactory.create(() -> DoubleArgumentType.doubleArg(minimum, maximum),
                type -> new DoubleRule(type, value, minimum, maximum), callback,
                (visitor, key, type) -> {
                    if (visitor instanceof FabricGameRuleVisitor fabric) fabric.visitDouble(key, type);
                });
    }
    public static <E extends Enum<E>> GameRules.Type<EnumRule<E>> createEnumRule(E value) {
        return createEnumRule(value, value.getDeclaringClass().getEnumConstants(), (server, rule) -> { });
    }
    public static <E extends Enum<E>> GameRules.Type<EnumRule<E>> createEnumRule(E value,
            BiConsumer<MinecraftServer, EnumRule<E>> callback) {
        return createEnumRule(value, value.getDeclaringClass().getEnumConstants(), callback);
    }
    public static <E extends Enum<E>> GameRules.Type<EnumRule<E>> createEnumRule(E value, E[] supported) {
        return createEnumRule(value, supported, (server, rule) -> { });
    }
    public static <E extends Enum<E>> GameRules.Type<EnumRule<E>> createEnumRule(E value, E[] supported,
            BiConsumer<MinecraftServer, EnumRule<E>> callback) {
        Objects.requireNonNull(value); Objects.requireNonNull(supported);
        if (supported.length == 0) throw new IllegalArgumentException("No supported enum values");
        return GameRuleTypeFactory.create(StringArgumentType::word,
                type -> new EnumRule<>(type, value, supported), callback,
                (visitor, key, type) -> {
                    if (visitor instanceof FabricGameRuleVisitor fabric) fabric.visitEnum(key, type);
                });
    }
}
