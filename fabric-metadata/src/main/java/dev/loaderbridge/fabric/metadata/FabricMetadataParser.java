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
    private static final java.util.regex.Pattern MOD_ID_PATTERN =
            java.util.regex.Pattern.compile("[a-z][a-z0-9_-]{1,63}");

    public FabricModMetadata parse(byte[] bytes) throws UnsafeJarException {
        final JsonObject root;
        try {
            root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new UnsafeJarException("Invalid fabric.mod.json: " + exception.getMessage());
        }

        int schemaVersion = requiredInteger(root, "schemaVersion");
        if (schemaVersion != 1) {
            throw new UnsafeJarException("Unsupported fabric.mod.json schemaVersion: " + schemaVersion);
        }

        String id = requiredString(root, "id");
        if (!MOD_ID_PATTERN.matcher(id).matches()) {
            throw new UnsafeJarException("Invalid Fabric mod id: " + id);
        }
        String version = requiredString(root, "version");

        return new FabricModMetadata(
                schemaVersion,
                id,
                version,
                optionalString(root, "name").orElse(id),
                parseEnvironment(optionalString(root, "environment").orElse("*")),
                parseEntrypoints(optionalObject(root, "entrypoints",
                        "Entrypoints must be an object")),
                new FabricDependencies(
                        parseConstraints(optionalObject(root, "depends",
                                "Dependency container must be an object")),
                        parseConstraints(optionalObject(root, "recommends",
                                "Dependency container must be an object")),
                        parseConstraints(optionalObject(root, "suggests",
                                "Dependency container must be an object")),
                        parseConstraints(optionalObject(root, "breaks",
                                "Dependency container must be an object")),
                        parseConstraints(optionalObject(root, "conflicts",
                                "Dependency container must be an object"))),
                parseProvides(root.get("provides")),
                parseMixins(optionalArray(root, "mixins",
                        "Mixin configs must be in an array")),
                optionalString(root, "accessWidener"),
                parseNestedJars(optionalArray(root, "jars",
                        "Jar entries must be in an array")),
                parseStringMap(optionalObject(root, "languageAdapters",
                        "Language adapters must be in an object"),
                        "Value of language adapter entry must be a string"),
                optionalString(root, "description").orElse(""),
                parsePeople(optionalArray(root, "authors", "List of people must be an array")),
                parsePeople(optionalArray(root, "contributors", "List of people must be an array")),
                parseStringMap(optionalObject(root, "contact", "Contact info must be an object"),
                        "Contact information entries must be a string"),
                parseStringValues(root.get("license")),
                parseIcons(root.get("icon")),
                parseCustom(optionalObject(root, "custom", "Custom values must be an object")));
    }

    private static Map<String, List<FabricEntrypoint>> parseEntrypoints(JsonObject object) throws UnsafeJarException {
        Map<String, List<FabricEntrypoint>> result = new LinkedHashMap<>();
        if (object == null) {
            return result;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            List<FabricEntrypoint> values = new ArrayList<>();
            if (!entry.getValue().isJsonArray()) {
                throw new UnsafeJarException("Entrypoint list must be an array: " + entry.getKey());
            }
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                if (isString(element)) {
                    values.add(new FabricEntrypoint("default", element.getAsString()));
                } else if (element.isJsonObject()) {
                    JsonObject value = element.getAsJsonObject();
                    values.add(new FabricEntrypoint(
                            optionalString(value, "adapter").orElse("default"),
                            requiredString(value, "value")));
                } else {
                    throw new UnsafeJarException(
                            "Entrypoint must be a string or object with value field");
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
                if (!isString(range)) {
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
            if (isString(element)) {
                result.add(new FabricMixin(element.getAsString(), "*"));
            } else if (element.isJsonObject()) {
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
            if (!element.isJsonObject()) {
                throw new UnsafeJarException("Invalid type for JAR entry");
            }
            result.add(requiredString(element.getAsJsonObject(), "file"));
        }
        return result;
    }

    private static List<String> parseProvides(JsonElement element) throws UnsafeJarException {
        List<String> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) return result;
        if (!element.isJsonArray()) {
            throw new UnsafeJarException("Provides must be an array");
        }
        for (JsonElement provided : element.getAsJsonArray()) {
            if (!provided.isJsonPrimitive() || !provided.getAsJsonPrimitive().isString()) {
                throw new UnsafeJarException("Provided id must be a string");
            }
            String id = provided.getAsString();
            if (!MOD_ID_PATTERN.matcher(id).matches()) {
                throw new UnsafeJarException("Invalid Fabric provides declaration: " + id);
            }
            result.add(id);
        }
        return result;
    }

    private static Map<String, String> parseStringMap(JsonObject object, String invalidValueMessage)
            throws UnsafeJarException {
        Map<String, String> result = new LinkedHashMap<>();
        if (object != null) {
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (!isString(entry.getValue())) {
                    throw new UnsafeJarException(invalidValueMessage);
                }
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return result;
    }

    private static List<FabricPerson> parsePeople(JsonArray array) throws UnsafeJarException {
        List<FabricPerson> result = new ArrayList<>();
        if (array == null) return result;
        for (JsonElement element : array) {
            if (isString(element)) {
                result.add(new FabricPerson(element.getAsString(), Map.of()));
            } else if (element.isJsonObject()) {
                JsonObject person = element.getAsJsonObject();
                result.add(new FabricPerson(requiredString(person, "name"),
                        parseStringMap(optionalObject(person, "contact",
                                "Contact info must be an object"),
                                "Contact information entries must be a string")));
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
        if (isString(element)) {
            result.put(0, element.getAsString());
            return result;
        }
        if (!element.isJsonObject()) throw new UnsafeJarException("Fabric icon must be a string or object");
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            try {
                int size = Integer.parseInt(entry.getKey());
                if (size <= 0) throw new NumberFormatException();
                if (!isString(entry.getValue())) {
                    throw new UnsafeJarException("Fabric icon path must be a string");
                }
                result.put(size, entry.getValue().getAsString());
            } catch (NumberFormatException exception) {
                throw new UnsafeJarException("Invalid Fabric icon size: " + entry.getKey());
            }
        }
        if (result.isEmpty()) {
            throw new UnsafeJarException("Fabric icon object must not be empty");
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
        JsonElement value = required(object, key);
        if (!isString(value)) {
            throw new UnsafeJarException("fabric.mod.json field " + key + " must be a string");
        }
        return value.getAsString();
    }

    private static int requiredInteger(JsonObject object, String key) throws UnsafeJarException {
        JsonElement value = required(object, key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new UnsafeJarException("fabric.mod.json field " + key + " must be a number");
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new UnsafeJarException("fabric.mod.json field " + key + " must be an integer");
        }
    }

    private static Optional<String> optionalString(JsonObject object, String key)
            throws UnsafeJarException {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return Optional.empty();
        if (!isString(value)) {
            throw new UnsafeJarException("fabric.mod.json field " + key + " must be a string");
        }
        return Optional.of(value.getAsString());
    }

    private static JsonObject optionalObject(JsonObject object, String key, String message)
            throws UnsafeJarException {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return null;
        if (!value.isJsonObject()) throw new UnsafeJarException(message);
        return value.getAsJsonObject();
    }

    private static JsonArray optionalArray(JsonObject object, String key, String message)
            throws UnsafeJarException {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return null;
        if (!value.isJsonArray()) throw new UnsafeJarException(message);
        return value.getAsJsonArray();
    }

    private static boolean isString(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }
}
