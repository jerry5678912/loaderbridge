package net.fabricmc.fabric.api.gamerule.v1;

import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;
import net.fabricmc.fabric.api.gamerule.v1.rule.EnumRule;
import net.minecraft.world.level.GameRules;

public interface FabricGameRuleVisitor extends GameRules.GameRuleTypeVisitor {
    default void visitDouble(GameRules.Key<DoubleRule> key, GameRules.Type<DoubleRule> type) { }
    default <E extends Enum<E>> void visitEnum(GameRules.Key<EnumRule<E>> key,
            GameRules.Type<EnumRule<E>> type) { }
}
