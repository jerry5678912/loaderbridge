package dev.loaderbridge.fabric.api.gamerule;

import static org.assertj.core.api.Assertions.assertThat;

import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;
import net.fabricmc.fabric.api.gamerule.v1.rule.EnumRule;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import org.junit.jupiter.api.Test;

class FabricGameRuleContractTest {
    private enum Mode { FIRST, SECOND }

    @Test
    void providerPinsEveryOfficialPublicType() {
        var descriptor = new FabricGameRuleBridgeProvider().descriptor();
        assertThat(descriptor.contractVersion()).isEqualTo("fabric-game-rule-api-v1:1.0.53");
        assertThat(descriptor.implementationVersion()).isEqualTo("1.0.53+6ced4dd919-loaderbridge.1");
        assertThat(descriptor.providedClasses()).hasSize(7)
                .contains("net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory",
                        "net.fabricmc.fabric.api.gamerule.v1.rule.EnumRule");
    }

    @Test
    void createsAndCopiesEveryRuleFamily() {
        String suffix = Long.toUnsignedString(System.nanoTime());
        var boolKey = GameRuleRegistry.register("loaderbridgeBool" + suffix,
                GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
        var intKey = GameRuleRegistry.register("loaderbridgeInt" + suffix,
                GameRules.Category.MISC, GameRuleFactory.createIntRule(4, 0, 8));
        GameRules.Key<DoubleRule> doubleKey = GameRuleRegistry.register("loaderbridgeDouble" + suffix,
                GameRules.Category.MISC, GameRuleFactory.createDoubleRule(1.5, 0.0, 2.0));
        GameRules.Key<EnumRule<Mode>> enumKey = GameRuleRegistry.register("loaderbridgeEnum" + suffix,
                GameRules.Category.MISC, GameRuleFactory.createEnumRule(Mode.FIRST));
        GameRules rules = new GameRules();

        assertThat(rules.getRule(boolKey).get()).isTrue();
        assertThat(rules.getRule(intKey).get()).isEqualTo(4);
        assertThat(rules.getRule(doubleKey).get()).isEqualTo(1.5);
        assertThat(rules.getRule(enumKey).get()).isEqualTo(Mode.FIRST);
        rules.getRule(enumKey).cycle();
        assertThat(rules.getRule(enumKey).get()).isEqualTo(Mode.SECOND);
    }

    @Test
    void customCategoriesAreAttachedToRegisteredKeys() {
        String suffix = Long.toUnsignedString(System.nanoTime());
        CustomGameRuleCategory category = new CustomGameRuleCategory(
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "test"), Component.literal("Test"));
        var key = GameRuleRegistry.register("loaderbridgeCategory" + suffix, category,
                GameRuleFactory.createBooleanRule(false));
        assertThat(CustomGameRuleCategory.getCategory(key)).contains(category);
        assertThat(GameRuleRegistry.hasRegistration(key.getId())).isTrue();
    }
}
