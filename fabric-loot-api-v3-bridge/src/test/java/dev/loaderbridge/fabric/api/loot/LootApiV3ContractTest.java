package dev.loaderbridge.fabric.api.loot;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.loot.v3.FabricLootPoolBuilder;
import net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.junit.jupiter.api.Test;

class LootApiV3ContractTest {
    @Test
    void providerPinsPublicContractAndDependencies() {
        var descriptor = new FabricLootApiV3BridgeProvider().descriptor();
        assertThat(descriptor.contractVersion()).isEqualTo("fabric-loot-api-v3:1.0.3");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("1.0.3+3f89f5a519-loaderbridge.1");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-loot-api-v3", "1.0.3+3f89f5a519");
        assertThat(descriptor.requiredModules()).containsExactlyInAnyOrder(
                "fabric-api-base-bridge", "fabric-resource-loader-v0-bridge");
        assertThat(descriptor.providedClasses()).hasSize(7);
    }

    @Test
    void sourceBuiltinSemanticsMatchFabric() {
        assertThat(LootTableSource.VANILLA.isBuiltin()).isTrue();
        assertThat(LootTableSource.MOD.isBuiltin()).isTrue();
        assertThat(LootTableSource.DATA_PACK.isBuiltin()).isFalse();
        assertThat(LootTableSource.REPLACED.isBuiltin()).isFalse();
    }

    @Test
    void replaceAndModifyEventsFanOutInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        LootTableEvents.REPLACE.register((key, original, source, registries) -> {
            calls.add("replace-first");
            return null;
        });
        LootTableEvents.REPLACE.register((key, original, source, registries) -> {
            calls.add("replace-second");
            return null;
        });
        AtomicInteger modifies = new AtomicInteger();
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> modifies.incrementAndGet());
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> modifies.incrementAndGet());
        LootTable replacement = LootTableEvents.REPLACE.invoker()
                .replaceLootTable(null, null, LootTableSource.VANILLA, null);
        LootTableEvents.MODIFY.invoker().modifyLootTable(
                null, null, LootTableSource.REPLACED, null);

        assertThat(replacement).isNull();
        assertThat(calls).containsExactly("replace-first", "replace-second");
        assertThat(modifies).hasValue(2);
    }

    @Test
    void exposesEveryPinnedBuilderOverload() throws Exception {
        assertMethods(FabricLootPoolBuilder.class,
                signature("with", LootPoolEntryContainer.class),
                signature("with", Collection.class),
                signature("conditionally", LootItemCondition.class),
                signature("conditionally", Collection.class),
                signature("apply", LootItemFunction.class),
                signature("apply", Collection.class),
                signature("copyOf", LootPool.class));
        assertMethods(FabricLootTableBuilder.class,
                signature("pool", LootPool.class),
                signature("pools", Collection.class),
                signature("apply", LootItemFunction.class),
                signature("apply", Collection.class),
                signature("modifyPools", java.util.function.Consumer.class),
                signature("copyOf", LootTable.class));
    }

    private static void assertMethods(Class<?> type, String... expected) {
        assertThat(List.of(type.getDeclaredMethods()).stream().map(LootApiV3ContractTest::signature))
                .contains(expected);
    }

    private static String signature(String name, Class<?>... parameters) {
        return name + List.of(parameters);
    }

    private static String signature(Method method) {
        return signature(method.getName(), method.getParameterTypes());
    }
}
