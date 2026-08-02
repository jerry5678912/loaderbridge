package dev.loaderbridge.fabric.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.metadata.CustomValue;

final class BridgeCustomValue implements CustomValue {
    private final JsonElement value;

    private BridgeCustomValue(JsonElement value) {
        this.value = value;
    }

    static CustomValue parse(String json) {
        return new BridgeCustomValue(JsonParser.parseString(json));
    }

    @Override public CvType getType() {
        if (value.isJsonObject()) return CvType.OBJECT;
        if (value.isJsonArray()) return CvType.ARRAY;
        if (value.isJsonNull()) return CvType.NULL;
        if (value.getAsJsonPrimitive().isBoolean()) return CvType.BOOLEAN;
        if (value.getAsJsonPrimitive().isNumber()) return CvType.NUMBER;
        return CvType.STRING;
    }

    @Override public CvObject getAsObject() {
        JsonObject object = value.getAsJsonObject();
        return new CvObject() {
            @Override public int size() { return object.size(); }
            @Override public boolean containsKey(String key) { return object.has(key); }
            @Override public CustomValue get(String key) {
                JsonElement selected = object.get(key);
                return selected == null ? null : new BridgeCustomValue(selected);
            }
            @Override public Iterator<Map.Entry<String, CustomValue>> iterator() {
                Map<String, CustomValue> values = new LinkedHashMap<>();
                object.entrySet().forEach(entry -> values.put(
                        entry.getKey(), new BridgeCustomValue(entry.getValue())));
                return values.entrySet().iterator();
            }
            @Override public CvType getType() { return BridgeCustomValue.this.getType(); }
            @Override public CvObject getAsObject() { return this; }
            @Override public CvArray getAsArray() { return BridgeCustomValue.this.getAsArray(); }
            @Override public String getAsString() { return BridgeCustomValue.this.getAsString(); }
            @Override public Number getAsNumber() { return BridgeCustomValue.this.getAsNumber(); }
            @Override public boolean getAsBoolean() { return BridgeCustomValue.this.getAsBoolean(); }
        };
    }

    @Override public CvArray getAsArray() {
        JsonArray array = value.getAsJsonArray();
        return new CvArray() {
            @Override public int size() { return array.size(); }
            @Override public CustomValue get(int index) { return new BridgeCustomValue(array.get(index)); }
            @Override public Iterator<CustomValue> iterator() {
                List<CustomValue> values = new ArrayList<>(array.size());
                array.forEach(element -> values.add(new BridgeCustomValue(element)));
                return values.iterator();
            }
            @Override public CvType getType() { return BridgeCustomValue.this.getType(); }
            @Override public CvObject getAsObject() { return BridgeCustomValue.this.getAsObject(); }
            @Override public CvArray getAsArray() { return this; }
            @Override public String getAsString() { return BridgeCustomValue.this.getAsString(); }
            @Override public Number getAsNumber() { return BridgeCustomValue.this.getAsNumber(); }
            @Override public boolean getAsBoolean() { return BridgeCustomValue.this.getAsBoolean(); }
        };
    }

    @Override public String getAsString() { return value.getAsString(); }
    @Override public Number getAsNumber() { return value.getAsNumber(); }
    @Override public boolean getAsBoolean() { return value.getAsBoolean(); }
}
