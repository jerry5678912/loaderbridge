package dev.loaderbridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FabricClientModelRegistrationTest {
    @Test
    void recordsOnlyItemsAddedByFabricEntrypoints() {
        var tracker = new FabricClientModelRegistration.Tracker();

        tracker.begin(List.of("minecraft:air", "forge:test_item"));
        tracker.complete(List.of(
                "minecraft:air", "forge:test_item", "example:new_block", "example:new_item"));

        assertThat(tracker.newItems())
                .containsExactlyInAnyOrder("example:new_block", "example:new_item");
    }
}
