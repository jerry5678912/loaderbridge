package dev.loaderbridge.fabric.api.recipe;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricRecipeApiV1BridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-recipe-api-v1-bridge",
            "fabric-recipe-api-v1:5.0.16",
            "5.0.16+2475392c19-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient",
                    "net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer",
                    "net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients",
                    "net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient"),
            Map.of("fabric-recipe-api-v1", "5.0.16+2475392c19"),
            Set.of("fabric-networking-api-v1-bridge"));

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
