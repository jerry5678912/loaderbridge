package dev.loaderbridge.fabric.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class FabricMetadataParser {
    public FabricModMetadata parse(byte[] bytes) throws UnsafeJarException {
        final JsonObject root;
        try {
            root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new UnsafeJarException("Invalid fabric.mod.json: " + exception.getMessage());
        }

        int schemaVersion = required(root, "schemaVersion").getAsInt();
        if (schemaVersion != 1) {
            throw new UnsafeJarException("Unsupported fabric.mod.json schemaVersion: " + schemaVersion);
        }

        String id = requiredString(root, "id");
        if (!id.matches("[a-z][a-z0-9_-]{1,63}")) {
            throw new UnsafeJarException("Invalid Fabric mod id: " + id);
        }
        String version = requiredString(root, "version");

        return new FabricModMetadata(
                schemaVersion,
                id,
                version,
                optionalString(root, "name").orElse(id),
                parseEnvironment(optionalString(root, "environment").orElse("*")),
                parseEntrypoints(root.getAsJsonObject("entrypoints")),
                new FabricDependencies(
                        parseConstraints(root.getAsJsonObject("depends")),
                        parseConstraints(root.getAsJsonObject("recommends")),
                        parseConstraints(root.getAsJsonObject("suggests")),
                        parseConstraints(root.getAsJsonObject("breaks")),
                        parseConstraints(root.getAsJsonObject("conflicts"))),
                parseStringArray(root.getAsJsonArray("provides")),
                parseMixins(root.getAsJsonArray("mixins")),
                optionalString(root, "accessWidener"),
                parseNestedJars(root.getAsJsonArray("jars")),
                parseStringMap(root.getAsJsonObject("languageAdapters")),
                optionalString(root, "description").orElse(""),
                parsePeople(root.getAsJsonArray("authors")),
                parsePeople(root.getAsJsonArray("contributors")),
                parseStringMap(root.getAsJsonObject("contact")),
                parseStringValues(root.get("license")),
                parseIcons(root.get("icon")),
                parseCustom(root.getAsJsonObject("custom")));
    }

    private static Map<String, List<FabricEntrypoint>> parseEntrypoints(JsonObject object) throws UnsafeJarException {
        Map<String, List<FabricEntrypoint>> result = new LinkedHashMap<>();
        if (object == null) {
            return result;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            List<FabricEntrypoint> values = new ArrayList<>();
            for (JsonElement element : asArray(entry.getValue())) {
                if (element.isJsonPrimitive()) {
                    values.add(new FabricEntrypoint("default", element.getAsString()));
                } else {
                    JsonObject value = element.getAsJsonObject();
                    values.add(new FabricEntrypoint(
                            optionalString(value, "adapter").orElse("default"),
                            requiredString(value, "value")));
                }
            }
            result.put(entry.getKey(), values);
        }
        return result;
    }

    private static Map<String, List<String>> parseConstraints(JsonObject object) throws UnsafeJarException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (object == null) {
            return result;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            List<String> ranges = new ArrayList<>();
            for (JsonElement range : asArray(entry.getValue())) {
                if (!range.isJsonPrimitive()) {
                    throw new UnsafeJarException("Dependency range for " + entry.getKey() + " must be a string");
                }
                ranges.add(range.getAsString());
            }
            result.put(entry.getKey(), ranges);
        }
        return result;
    }

    private static List<FabricMixin> parseMixins(JsonArray array) throws UnsafeJarException {
        List<FabricMixin> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                result.add(new FabricMixin(element.getAsString(), "*"));
            } else {
                JsonObject mixin = element.getAsJsonObject();
                result.add(new FabricMixin(
                        requiredString(mixin, "config"),
                        parseEnvironment(optionalString(mixin, "environment").orElse("*"))));
            }
        }
        return result;
    }

    private static String parseEnvironment(String value) throws UnsafeJarException {
        String environment = value.toLowerCase(Locale.ROOT);
        return switch (environment) {
            case "", "*" -> "*";
            case "client", "server" -> environment;
            default -> throw new UnsafeJarException("Invalid environment type: " + environment);
        };
    }

    private static List<String> parseNestedJars(JsonArray array) throws UnsafeJarException {
        List<String> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (JsonElement element : array) {
            result.add(requiredString(element.getAsJsonObject(), "file"));
        }
        return result;
    }

    private static List<String> parseStringArray(JsonArray array) {
        List<String> result = new ArrayList<>();
        if (array != null) {
            array.forEach(element -> result.add(element.getAsString()));
        }
        return result;
    }

    private static Map<String, String> parseStringMap(JsonObject object) {
        Map<String, String> result = new LinkedHashMap<>();
        if (object != null) {
            object.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
        }
        return result;
    }

    private static List<FabricPerson> parsePeople(JsonArray array) throws UnsafeJarException {
        List<FabricPerson> result = new ArrayList<>();
        if (array == null) return result;
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                result.add(new FabricPerson(element.getAsString(), Map.of()));
            } else if (element.isJsonObject()) {
                JsonObject person = element.getAsJsonObject();
                result.add(new FabricPerson(requiredString(person, "name"),
                        parseStringMap(person.getAsJsonObject("contact"))));
            } else {
                throw new UnsafeJarException("Fabric author/contributor must be a string or object");
            }
        }
        return result;
    }

    private static List<String> parseStringValues(JsonElement element) throws UnsafeJarException {
        List<String> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) return result;
        for (JsonElement value : asArray(element)) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new UnsafeJarException("Fabric metadata value must be a string");
            }
            result.add(value.getAsString());
        }
        return result;
    }

    private static Map<Integer, String> parseIcons(JsonElement element) throws UnsafeJarException {
        Map<Integer, String> result = new LinkedHashMap<>();
        if (element == null || element.isJsonNull()) return result;
        if (element.isJsonPrimitive()) {
            result.put(0, element.getAsString());
            return result;
        }
        if (!element.isJsonObject()) throw new UnsafeJarException("Fabric icon must be a string or object");
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            try {
                int size = Integer.parseInt(entry.getKey());
                if (size <= 0) throw new NumberFormatException();
                result.put(size, entry.getValue().getAsString());
            } catch (NumberFormatException | UnsupportedOperationException exception) {
                throw new UnsafeJarException("Invalid Fabric icon size: " + entry.getKey());
            }
        }
        return result;
    }

    private static Map<String, String> parseCustom(JsonObject object) {
        Map<String, String> result = new LinkedHashMap<>();
        if (object != null) {
            object.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().toString()));
        }
        return result;
    }

    private static JsonArray asArray(JsonElement element) {
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        JsonArray array = new JsonArray();
        array.add(element);
        return array;
    }

    private static JsonElement required(JsonObject object, String key) throws UnsafeJarException {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            throw new UnsafeJarException("fabric.mod.json is missing required field: " + key);
        }
        return value;
    }

    private static String requiredString(JsonObject object, String key) throws UnsafeJarException {
        try {
            return required(object, key).getAsString();
        } catch (RuntimeException exception) {
            throw new UnsafeJarException("fabric.mod.json field " + key + " must be a string");
        }
    }

    private static Optional<String> optionalString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? Optional.empty() : Optional.of(value.getAsString());
    }
}
