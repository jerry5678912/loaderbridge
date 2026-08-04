package dev.loaderbridge.fabric.entity.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.FabricElytraItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class EntityEventsV1ContractTest {
    @Test
    void registersTheClientElytraStartMixinSeparately() throws Exception {
        try (var stream = EntityEventsV1ContractTest.class.getResourceAsStream(
                "/loaderbridge.fabric-entity-events-v1.mixins.json")) {
            assertThat(stream).isNotNull();
            String config = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(config).contains("\"client\": [\"LocalPlayerMixin\"]");
        }
    }

    @Test
    void providerPinsCompletePublicSurface() {
        var descriptor = new FabricEntityEventsV1BridgeProvider().descriptor();
        assertThat(descriptor.contractVersion()).isEqualTo("fabric-entity-events-v1:1.8.0");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("1.8.0+2b27e0a419-loaderbridge.2");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-entity-events-v1", "1.8.0+2b27e0a419");
        assertThat(descriptor.providedClasses()).hasSize(33);
        assertThat(descriptor.requiredModules()).containsExactly("fabric-api-base-bridge");
    }

    @Test
    @SuppressWarnings("deprecation")
    void exposesPinnedCallbackDescriptors() {
        assertMethod(EntityElytraEvents.Allow.class, "allowElytraFlight", LivingEntity.class);
        assertMethod(EntityElytraEvents.Custom.class, "useCustomElytra", LivingEntity.class, boolean.class);
        assertMethods(FabricElytraItem.class,
                signature("useCustomElytra", LivingEntity.class, ItemStack.class, boolean.class),
                signature("doVanillaElytraTick", LivingEntity.class, ItemStack.class));

        assertMethod(EntitySleepEvents.AllowSleeping.class, "allowSleep", Player.class, BlockPos.class);
        assertMethod(EntitySleepEvents.StartSleeping.class, "onStartSleeping", LivingEntity.class, BlockPos.class);
        assertMethod(EntitySleepEvents.StopSleeping.class, "onStopSleeping", LivingEntity.class, BlockPos.class);
        assertMethod(EntitySleepEvents.AllowBed.class, "allowBed",
                LivingEntity.class, BlockPos.class, BlockState.class, boolean.class);
        assertMethod(EntitySleepEvents.AllowSleepTime.class, "allowSleepTime",
                Player.class, BlockPos.class, boolean.class);
        assertMethod(EntitySleepEvents.AllowNearbyMonsters.class, "allowNearbyMonsters",
                Player.class, BlockPos.class, boolean.class);
        assertMethod(EntitySleepEvents.AllowResettingTime.class, "allowResettingTime", Player.class);
        assertMethod(EntitySleepEvents.ModifySleepingDirection.class, "modifySleepDirection",
                LivingEntity.class, BlockPos.class, Direction.class);
        assertMethod(EntitySleepEvents.AllowSettingSpawn.class, "allowSettingSpawn", Player.class, BlockPos.class);
        assertMethod(EntitySleepEvents.SetBedOccupationState.class, "setBedOccupationState",
                LivingEntity.class, BlockPos.class, BlockState.class, boolean.class);
        assertMethod(EntitySleepEvents.ModifyWakeUpPosition.class, "modifyWakeUpPosition",
                LivingEntity.class, BlockPos.class, BlockState.class, Vec3.class);

        assertMethod(ServerEntityCombatEvents.AfterKilledOtherEntity.class,
                "afterKilledOtherEntity", ServerLevel.class, Entity.class, LivingEntity.class);
        assertMethod(ServerEntityWorldChangeEvents.AfterEntityChange.class, "afterChangeWorld",
                Entity.class, Entity.class, ServerLevel.class, ServerLevel.class);
        assertMethod(ServerEntityWorldChangeEvents.AfterPlayerChange.class, "afterChangeWorld",
                ServerPlayer.class, ServerLevel.class, ServerLevel.class);
        assertMethod(ServerLivingEntityEvents.AllowDamage.class, "allowDamage",
                LivingEntity.class, DamageSource.class, float.class);
        assertMethod(ServerLivingEntityEvents.AfterDamage.class, "afterDamage",
                LivingEntity.class, DamageSource.class, float.class, float.class, boolean.class);
        assertMethod(ServerLivingEntityEvents.AllowDeath.class, "allowDeath",
                LivingEntity.class, DamageSource.class, float.class);
        assertMethod(ServerLivingEntityEvents.AfterDeath.class, "afterDeath",
                LivingEntity.class, DamageSource.class);
        assertMethod(ServerLivingEntityEvents.MobConversion.class, "onConversion",
                Mob.class, Mob.class, boolean.class);
        assertMethod(ServerPlayerEvents.CopyFrom.class, "copyFromPlayer",
                ServerPlayer.class, ServerPlayer.class, boolean.class);
        assertMethod(ServerPlayerEvents.AfterRespawn.class, "afterRespawn",
                ServerPlayer.class, ServerPlayer.class, boolean.class);
        assertMethod(ServerPlayerEvents.Join.class, "onJoin", ServerPlayer.class);
        assertMethod(ServerPlayerEvents.Leave.class, "onLeave", ServerPlayer.class);
        assertMethod(ServerPlayerEvents.AllowDeath.class, "allowDeath",
                ServerPlayer.class, DamageSource.class, float.class);
    }

    @Test
    void aggregationMatchesFabricShortCircuitAndPassRules() {
        EntitySleepEvents.ALLOW_BED.register((entity, pos, state, vanilla) -> InteractionResult.PASS);
        EntitySleepEvents.ALLOW_BED.register((entity, pos, state, vanilla) -> InteractionResult.FAIL);
        assertThat(EntitySleepEvents.ALLOW_BED.invoker().allowBed(null, null, null, true))
                .isEqualTo(InteractionResult.FAIL);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> true);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> false);
        assertThat(ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(null, null, 1))
                .isFalse();
    }

    private static void assertMethod(Class<?> type, String name, Class<?>... parameters) {
        assertThat(Arrays.stream(type.getDeclaredMethods()).map(EntityEventsV1ContractTest::signature))
                .contains(signature(name, parameters));
    }

    private static void assertMethods(Class<?> type, String... expected) {
        assertThat(Arrays.stream(type.getDeclaredMethods()).map(EntityEventsV1ContractTest::signature))
                .contains(expected);
    }

    private static String signature(String name, Class<?>... parameters) {
        return name + List.of(parameters);
    }

    private static String signature(Method method) {
        return signature(method.getName(), method.getParameterTypes());
    }
}
