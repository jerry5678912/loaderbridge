package dev.loaderbridge.fabric.api.tag.v1;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Structural translation from Fabric's tag-removal key to Forge's equivalent codec field. */
public final class FabricTagJson {
    public static JsonElement translateRemove(JsonElement source) {
        if (!source.isJsonObject()) return source;
        JsonObject object = source.getAsJsonObject();
        JsonElement fabricRemove = object.remove("fabric:remove");
        if (fabricRemove == null) return source;
        if (!fabricRemove.isJsonArray()) {
            object.add("fabric:remove", fabricRemove);
            return source;
        }
        JsonArray merged = new JsonArray();
        JsonElement forgeRemove = object.get("remove");
        if (forgeRemove != null && !forgeRemove.isJsonArray()) {
            object.add("fabric:remove", fabricRemove);
            return source;
        }
        if (forgeRemove != null) forgeRemove.getAsJsonArray().forEach(merged::add);
        fabricRemove.getAsJsonArray().forEach(merged::add);
        object.add("remove", merged);
        return source;
    }

    private FabricTagJson() { }
}
