package dev.loaderbridge.fabric.api.tag.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonParser;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FabricTagApiContractTest {
    @Test
    void exposesPinnedModuleAndDependencies() {
        var descriptor = new FabricTagApiBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion())
                .isEqualTo("1.3.0+1eb36c0719-loaderbridge.1");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-tag-api-v1", "1.3.0+1eb36c0719");
        assertThat(descriptor.providedClasses())
                .containsExactly("net.fabricmc.fabric.api.tag.v1.FabricTagFile");
        assertThat(descriptor.requiredModules()).containsExactlyInAnyOrderElementsOf(Set.of(
                "fabric-api-base-bridge", "fabric-resource-loader-v0-bridge"));
    }

    @Test
    void aliasesFabricRemoveIntoForgesNativeOrderedRemovalField() {
        var translated = FabricTagJson.translateRemove(JsonParser.parseString("""
                {"values":["minecraft:stone"],"remove":["minecraft:dirt"],
                 "fabric:remove":["#minecraft:logs","minecraft:granite"]}
                """)).getAsJsonObject();

        assertThat(translated.has("fabric:remove")).isFalse();
        assertThat(translated.getAsJsonArray("remove").asList())
                .extracting(Object::toString)
                .containsExactly("\"minecraft:dirt\"", "\"#minecraft:logs\"",
                        "\"minecraft:granite\"");
    }

    @Test
    void leavesMalformedFabricRemoveForTheCodecToReject() {
        var source = JsonParser.parseString("{\"values\":[],\"fabric:remove\":42}");

        assertThat(FabricTagJson.translateRemove(source)).isSameAs(source);
        assertThat(source.getAsJsonObject().get("fabric:remove").getAsInt()).isEqualTo(42);
    }
}
