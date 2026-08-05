package dev.loaderbridge.fabric.api.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.fml.common.Mod;
import org.junit.jupiter.api.Test;

class FabricInteractionEventsContractTest {
    @Test
    void providerPinsOnlyTheImplementedServerSharedSurface() {
        var descriptor = new FabricInteractionEventsBridgeProvider().descriptor();
        assertThat(descriptor.contractVersion())
                .isEqualTo("fabric-events-interaction-v0:0.7.14");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("0.7.14+ba9dae0619-loaderbridge.2");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-events-interaction-v0", "0.7.14+ba9dae0619");
        assertThat(descriptor.requiredModules()).containsExactlyInAnyOrder(
                "fabric-api-base-bridge", "fabric-networking-api-v1-bridge");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrder(
                "net.fabricmc.fabric.api.block.BlockAttackInteractionAware",
                "net.fabricmc.fabric.api.entity.FakePlayer",
                "net.fabricmc.fabric.api.event.player.AttackBlockCallback",
                "net.fabricmc.fabric.api.event.player.AttackEntityCallback",
                "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents",
                "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents$After",
                "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents$Before",
                "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents$Canceled",
                "net.fabricmc.fabric.api.event.player.UseBlockCallback",
                "net.fabricmc.fabric.api.event.player.UseEntityCallback",
                "net.fabricmc.fabric.api.event.player.UseItemCallback");
    }

    @Test
    void fakePlayerExposesThePinnedConstructionAndOverrideContract() throws Exception {
        assertThat(FakePlayer.class.getDeclaredField("DEFAULT_UUID").getType())
                .isEqualTo(java.util.UUID.class);
        assertThat(FakePlayer.class.getDeclaredMethod("get",
                net.minecraft.server.level.ServerLevel.class).getReturnType())
                .isEqualTo(FakePlayer.class);
        assertThat(FakePlayer.class.getDeclaredMethod("get",
                net.minecraft.server.level.ServerLevel.class,
                com.mojang.authlib.GameProfile.class).getReturnType())
                .isEqualTo(FakePlayer.class);
        assertThat(FakePlayer.class.getDeclaredConstructor(
                net.minecraft.server.level.ServerLevel.class,
                com.mojang.authlib.GameProfile.class).getModifiers())
                .matches(modifiers -> java.lang.reflect.Modifier.isProtected(modifiers));
        assertThat(FakePlayer.class.getDeclaredMethod("tick")).isNotNull();
        assertThat(FakePlayer.class.getDeclaredMethod("updateOptions",
                net.minecraft.server.level.ClientInformation.class)).isNotNull();
        assertThat(FakePlayer.class.getDeclaredMethod("isInvulnerableTo",
                net.minecraft.world.damagesource.DamageSource.class)).isNotNull();
        assertThat(FakePlayer.class.getDeclaredMethod("openMenu",
                net.minecraft.world.MenuProvider.class)).isNotNull();
    }

    @Test
    void forgeHostContainerMatchesPackagedIdentity() {
        assertThat(FabricInteractionEventsBridgeMod.class.getAnnotation(Mod.class).value())
                .isEqualTo("loaderbridge_fabric_events_interaction_v0");
    }

    @Test
    void actionCallbacksStopAtTheFirstNonPassResult() {
        AtomicInteger attackCalls = new AtomicInteger();
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            attackCalls.incrementAndGet();
            return InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            attackCalls.incrementAndGet();
            return InteractionResult.CONSUME;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            attackCalls.incrementAndGet();
            return InteractionResult.FAIL;
        });
        assertThat(AttackBlockCallback.EVENT.invoker()
                .interact(null, null, null, null, null)).isEqualTo(InteractionResult.CONSUME);
        assertThat(attackCalls).hasValue(2);

        AtomicInteger useCalls = new AtomicInteger();
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            useCalls.incrementAndGet();
            return InteractionResult.SUCCESS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            useCalls.incrementAndGet();
            return InteractionResult.FAIL;
        });
        assertThat(UseBlockCallback.EVENT.invoker().interact(null, null, null, null))
                .isEqualTo(InteractionResult.SUCCESS);
        assertThat(useCalls).hasValue(1);
    }

    @Test
    void blockBreakBeforeShortCircuitsWhileTerminalEventsFanOut() {
        AtomicInteger beforeCalls = new AtomicInteger();
        AtomicInteger afterCalls = new AtomicInteger();
        AtomicInteger canceledCalls = new AtomicInteger();
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            beforeCalls.incrementAndGet();
            return false;
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            beforeCalls.incrementAndGet();
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register(
                (world, player, pos, state, entity) -> afterCalls.incrementAndGet());
        PlayerBlockBreakEvents.AFTER.register(
                (world, player, pos, state, entity) -> afterCalls.incrementAndGet());
        PlayerBlockBreakEvents.CANCELED.register(
                (world, player, pos, state, entity) -> canceledCalls.incrementAndGet());
        PlayerBlockBreakEvents.CANCELED.register(
                (world, player, pos, state, entity) -> canceledCalls.incrementAndGet());

        assertThat(PlayerBlockBreakEvents.BEFORE.invoker()
                .beforeBlockBreak(null, null, null, null, null)).isFalse();
        PlayerBlockBreakEvents.AFTER.invoker()
                .afterBlockBreak(null, null, null, null, null);
        PlayerBlockBreakEvents.CANCELED.invoker()
                .onBlockBreakCanceled(null, null, null, null, null);
        assertThat(beforeCalls).hasValue(1);
        assertThat(afterCalls).hasValue(2);
        assertThat(canceledCalls).hasValue(2);
    }
}
