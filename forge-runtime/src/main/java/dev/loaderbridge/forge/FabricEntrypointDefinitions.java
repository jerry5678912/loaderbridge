package dev.loaderbridge.forge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Reads schema-v1 entrypoint declarations without loading their classes. */
final class FabricEntrypointDefinitions {
    private FabricEntrypointDefinitions() {
    }

    static List<Declaration> parse(JsonObject metadata) {
        JsonObject entrypoints = metadata.getAsJsonObject("entrypoints");
        if (entrypoints == null) {
            return List.of();
        }
        List<Declaration> declarations = new ArrayList<>();
        for (var keyed : entrypoints.entrySet()) {
            JsonElement value = keyed.getValue();
            Iterable<JsonElement> entries = value.isJsonArray()
                    ? value.getAsJsonArray() : List.of(value);
            for (JsonElement entry : entries) {
                if (entry.isJsonPrimitive()) {
                    declarations.add(new Declaration(
                            keyed.getKey(), "default", entry.getAsString()));
                } else {
                    JsonObject object = entry.getAsJsonObject();
                    declarations.add(new Declaration(
                            keyed.getKey(),
                            object.has("adapter") ? object.get("adapter").getAsString() : "default",
                            object.get("value").getAsString()));
                }
            }
        }
        return List.copyOf(declarations);
    }

    record Declaration(String key, String adapter, String value) {
    }
}
