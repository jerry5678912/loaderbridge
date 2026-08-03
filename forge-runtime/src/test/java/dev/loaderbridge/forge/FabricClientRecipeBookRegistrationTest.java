package dev.loaderbridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FabricClientRecipeBookRegistrationTest {
    @Test
    void recordsOnlyRecipeTypesAddedByFabricEntrypoints() {
        var tracker = new FabricClientRecipeBookRegistration.Tracker();

        tracker.begin(List.of("minecraft:crafting", "minecraft:smelting"));
        tracker.complete(List.of(
                "minecraft:crafting", "minecraft:smelting", "example:kiln_smelting"));

        assertThat(tracker.newRecipeTypes()).containsExactly("example:kiln_smelting");
    }
}
