package dev.loaderbridge.fabric.api.gamerule;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricGameRuleBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-game-rule-api-v1-bridge", "fabric-game-rule-api-v1:1.0.53",
            "1.0.53+6ced4dd919-loaderbridge.1", BridgeCapability.FABRIC_API,
            Set.of("net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory",
                    "net.fabricmc.fabric.api.gamerule.v1.FabricGameRuleVisitor",
                    "net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory",
                    "net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry",
                    "net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule",
                    "net.fabricmc.fabric.api.gamerule.v1.rule.EnumRule",
                    "net.fabricmc.fabric.api.gamerule.v1.rule.ValidateableRule"),
            Map.of("fabric-game-rule-api-v1", "1.0.53+6ced4dd919"), Set.of());
    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }
    @Override public Path artifact() throws IOException {
        try {
            Path path = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(path)) throw new IOException("LB-MODULE-002: not running from a JAR");
            return path;
        } catch (URISyntaxException exception) { throw new IOException(exception); }
    }
}
