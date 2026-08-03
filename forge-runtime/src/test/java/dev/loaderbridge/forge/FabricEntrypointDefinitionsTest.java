package dev.loaderbridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class FabricEntrypointDefinitionsTest {
    @Test
    void parsesLifecycleAndCustomEntrypointsInMetadataOrder() {
        var metadata = JsonParser.parseString("""
                {
                  "entrypoints": {
                    "fixture-api": [
                      "example.First",
                      {"adapter":"kotlin","value":"example.Second::INSTANCE"}
                    ],
                    "main": "example.Main"
                  }
                }
                """).getAsJsonObject();

        assertThat(FabricEntrypointDefinitions.parse(metadata))
                .containsExactly(
                        new FabricEntrypointDefinitions.Declaration(
                                "fixture-api", "default", "example.First"),
                        new FabricEntrypointDefinitions.Declaration(
                                "fixture-api", "kotlin", "example.Second::INSTANCE"),
                        new FabricEntrypointDefinitions.Declaration(
                                "main", "default", "example.Main"));
    }
}
