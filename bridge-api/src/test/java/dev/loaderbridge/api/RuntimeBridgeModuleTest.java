package dev.loaderbridge.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RuntimeBridgeModuleTest {
    @Test
    void rejectsPathStyleClassNames() {
        assertThatThrownBy(() -> new RuntimeBridgeModule("base", "1", "1.0",
                BridgeCapability.FABRIC_API, Set.of("net/fabricmc/Bad"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("binary names");
    }
}
