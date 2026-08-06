package dev.loaderbridge.fabric.api.dimension;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FabricDimensionsContractTest {
    @Test
    void exposesPinnedMetadataOnlyModuleAndApiBaseDependency() {
        var descriptor = new FabricDimensionsBridgeProvider().descriptor();

        assertThat(descriptor.contractVersion()).isEqualTo("fabric-dimensions-v1:4.0.1");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("4.0.1+65213ef819-loaderbridge.1");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-dimensions-v1", "4.0.1+65213ef819");
        assertThat(descriptor.providedClasses()).isEmpty();
        assertThat(descriptor.requiredModules()).isEqualTo(Set.of("fabric-api-base-bridge"));
    }

    @Test
    void malformedDimensionEntryDoesNotTurnValidMapIntoPartialFailure() {
        var codec = new FailSoftMapCodec<>(Codec.STRING, Codec.INT);

        var result = codec.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"valid\":7,\"removed\":{\"missing\":true}}"));

        assertThat(result.error()).isEmpty();
        assertThat(result.result().orElseThrow()).containsExactlyEntriesOf(Map.of("valid", 7));
    }
}
