package dev.loaderbridge.fabric.remap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.loaderbridge.fabric.metadata.UnsafeJarException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

/** Translates Mixin reference maps while retaining both pre- and post-remap lookup keys. */
final class MixinRefmapTransformer {
    byte[] transform(byte[] input, TinyMappingIndex mappings, String resource)
            throws UnsafeJarException {
        return transform(input, mappings, resource, Map.of());
    }

    byte[] transform(byte[] input, TinyMappingIndex mappings, String resource,
            Map<String, String> mixinTargetOwners) throws UnsafeJarException {
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(new String(input, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new JsonParseException("root is not an object");
            root = parsed.getAsJsonObject();
            transformMappings(root.get("mappings"), mappings, resource, mixinTargetOwners);
            JsonElement data = root.get("data");
            if (data != null) {
                if (!data.isJsonObject()) throw new JsonParseException("data is not an object");
                for (Map.Entry<String, JsonElement> context : data.getAsJsonObject().entrySet()) {
                    transformMappings(context.getValue(), mappings, resource, mixinTargetOwners);
                }
            }
        } catch (JsonParseException | IllegalStateException exception) {
            UnsafeJarException failure = new UnsafeJarException(
                    "LB-MIXIN-REFMAP-002: malformed refmap: " + resource);
            failure.initCause(exception);
            throw failure;
        }
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    private static void transformMappings(JsonElement value, TinyMappingIndex mappings,
            String resource, Map<String, String> mixinTargetOwners) throws UnsafeJarException {
        if (value == null) return;
        if (!value.isJsonObject()) {
            throw new UnsafeJarException("LB-MIXIN-REFMAP-002: mappings must be an object: "
                    + resource);
        }
        for (Map.Entry<String, JsonElement> mixin : value.getAsJsonObject().entrySet()) {
            if (!mixin.getValue().isJsonObject()) {
                throw new UnsafeJarException("LB-MIXIN-REFMAP-002: mixin mappings must be objects: "
                        + resource);
            }
            JsonObject entries = mixin.getValue().getAsJsonObject();
            String owner = mixinTargetOwners.get(mixin.getKey());
            if (owner != null) owner = mappings.sourceClass(owner);
            var originals = new ArrayList<>(entries.entrySet());
            for (Map.Entry<String, JsonElement> entry : originals) {
                if (!entry.getValue().isJsonPrimitive()
                        || !entry.getValue().getAsJsonPrimitive().isString()) {
                    throw new UnsafeJarException("LB-MIXIN-REFMAP-002: mapping values must be strings: "
                            + resource);
                }
                String translated = mappings.translateReference(entry.getValue().getAsString(), owner);
                entries.addProperty(entry.getKey(), translated);
                String runtimeKey = TinyMappingIndex.unqualified(translated);
                JsonElement collision = entries.get(runtimeKey);
                if (collision != null && !collision.getAsString().equals(translated)) {
                    throw new UnsafeJarException("LB-MIXIN-REFMAP-003: translated key collision in "
                            + resource + ": " + runtimeKey);
                }
                entries.addProperty(runtimeKey, translated);
            }
        }
    }
}
