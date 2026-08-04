package dev.loaderbridge.fabric.entity.events;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricEntityEventsV1BridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-entity-events-v1-bridge",
            "fabric-entity-events-v1:1.8.0",
            "1.8.0+2b27e0a419-loaderbridge.2",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents",
                    "net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents$Allow",
                    "net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents$Custom",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$AllowSleeping",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$StartSleeping",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$StopSleeping",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$AllowBed",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$AllowSleepTime",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$AllowNearbyMonsters",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$AllowResettingTime",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$ModifySleepingDirection",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$AllowSettingSpawn",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$SetBedOccupationState",
                    "net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents$ModifyWakeUpPosition",
                    "net.fabricmc.fabric.api.entity.event.v1.FabricElytraItem",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents$AfterKilledOtherEntity",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents$AfterEntityChange",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents$AfterPlayerChange",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents$AllowDamage",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents$AfterDamage",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents$AllowDeath",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents$AfterDeath",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents$MobConversion",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents$CopyFrom",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents$AfterRespawn",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents$Join",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents$Leave",
                    "net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents$AllowDeath"),
            Map.of("fabric-entity-events-v1", "1.8.0+2b27e0a419"),
            Set.of("fabric-api-base-bridge"));

    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }

    @Override public Path artifact() throws IOException {
        try {
            Path location = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(location) || !location.getFileName().toString().endsWith(".jar")) {
                throw new IOException("LB-MODULE-002: bridge module is not running from a JAR: " + location);
            }
            return location;
        } catch (URISyntaxException exception) {
            throw new IOException("LB-MODULE-002: invalid bridge module location", exception);
        }
    }
}
