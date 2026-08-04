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
    private final CvObject objectView;
    private final CvArray arrayView;

    private BridgeCustomValue(JsonElement value) {
        this.value = value;
        this.objectView = value.isJsonObject() ? objectView(value.getAsJsonObject()) : null;
        this.arrayView = value.isJsonArray() ? arrayView(value.getAsJsonArray()) : null;
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
        if (objectView == null) throw wrongType("Object");
        return objectView;
    }

    private CvObject objectView(JsonObject object) {
        Map<String, CustomValue> parsedValues = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> parsedValues.put(
                entry.getKey(), new BridgeCustomValue(entry.getValue())));
        Map<String, CustomValue> values = java.util.Collections.unmodifiableMap(parsedValues);
        return new CvObject() {
            @Override public int size() { return values.size(); }
            @Override public boolean containsKey(String key) { return values.containsKey(key); }
            @Override public CustomValue get(String key) { return values.get(key); }
            @Override public Iterator<Map.Entry<String, CustomValue>> iterator() {
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
        if (arrayView == null) throw wrongType("Array");
        return arrayView;
    }

    private CvArray arrayView(JsonArray array) {
        List<CustomValue> parsedValues = new ArrayList<>(array.size());
        array.forEach(element -> parsedValues.add(new BridgeCustomValue(element)));
        List<CustomValue> values = List.copyOf(parsedValues);
        return new CvArray() {
            @Override public int size() { return values.size(); }
            @Override public CustomValue get(int index) { return values.get(index); }
            @Override public Iterator<CustomValue> iterator() { return values.iterator(); }
            @Override public CvType getType() { return BridgeCustomValue.this.getType(); }
            @Override public CvObject getAsObject() { return BridgeCustomValue.this.getAsObject(); }
            @Override public CvArray getAsArray() { return this; }
            @Override public String getAsString() { return BridgeCustomValue.this.getAsString(); }
            @Override public Number getAsNumber() { return BridgeCustomValue.this.getAsNumber(); }
            @Override public boolean getAsBoolean() { return BridgeCustomValue.this.getAsBoolean(); }
        };
    }

    @Override public String getAsString() {
        if (getType() != CvType.STRING) throw wrongType("String");
        return value.getAsString();
    }

    @Override public Number getAsNumber() {
        if (getType() != CvType.NUMBER) throw wrongType("Number");
        return value.getAsDouble();
    }

    @Override public boolean getAsBoolean() {
        if (getType() != CvType.BOOLEAN) throw wrongType("Boolean");
        return value.getAsBoolean();
    }

    private ClassCastException wrongType(String target) {
        return new ClassCastException("can't convert " + getType().name() + " to " + target);
    }
}
