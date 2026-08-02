package dev.loaderbridge.scenario.yaml;

import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.scenario.CompatibilityScenario;
import dev.loaderbridge.scenario.ScenarioAction;
import dev.loaderbridge.scenario.ScenarioStep;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.schema.JsonSchema;

/** Strict YAML 1.2 loader that only maps basic collections into scenario records. */
public final class ScenarioYamlParser {
    private static final int MAXIMUM_BYTES = 1 << 20;
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "id", "description",
            "side", "mods", "steps");
    private static final Set<String> STEP_FIELDS = Set.of("id", "action", "timeout", "parameters");
    private static final LoadSettings SETTINGS = LoadSettings.builder().setLabel("LoaderBridge scenario")
            .setSchema(new JsonSchema()).setAllowDuplicateKeys(false).setAllowRecursiveKeys(false)
            .setAllowNonScalarKeys(false).setMaxAliasesForCollections(0).setCodePointLimit(MAXIMUM_BYTES)
            .build();

    public CompatibilityScenario parse(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("Scenario is not a regular file: " + path);
        }
        byte[] bytes;
        try (InputStream input = Files.newInputStream(path)) {
            bytes = input.readNBytes(MAXIMUM_BYTES + 1);
        }
        if (bytes.length > MAXIMUM_BYTES) {
            throw new IOException("Scenario exceeds the 1 MiB safety limit");
        }
        try {
            Object loaded = new Load(SETTINGS).loadFromString(new String(bytes,
                    java.nio.charset.StandardCharsets.UTF_8));
            return scenario(map(loaded, "document"));
        } catch (YamlEngineException | IllegalArgumentException exception) {
            throw new ScenarioFormatException("Invalid scenario YAML: " + exception.getMessage(), exception);
        }
    }

    private static CompatibilityScenario scenario(Map<?, ?> document) {
        rejectUnknown(document, ROOT_FIELDS, "scenario");
        int version = integer(document, "schemaVersion");
        String id = string(document, "id");
        String description = string(document, "description");
        BridgeEnvironment side = BridgeEnvironment.valueOf(string(document, "side")
                .toUpperCase(java.util.Locale.ROOT));
        List<String> mods = strings(document.get("mods"), "mods");
        Object stepsValue = required(document, "steps");
        if (!(stepsValue instanceof List<?> sourceSteps)) {
            throw new IllegalArgumentException("steps must be a list");
        }
        List<ScenarioStep> steps = new ArrayList<>();
        for (int index = 0; index < sourceSteps.size(); index++) {
            steps.add(step(map(sourceSteps.get(index), "steps[" + index + "]")));
        }
        return new CompatibilityScenario(version, id, description, side, mods, steps);
    }

    private static ScenarioStep step(Map<?, ?> source) {
        rejectUnknown(source, STEP_FIELDS, "step");
        String id = string(source, "id");
        ScenarioAction action = new ScenarioAction(string(source, "action"));
        Duration timeout = Duration.parse(string(source, "timeout"));
        Map<String, String> parameters = new LinkedHashMap<>();
        Object parameterValue = source.get("parameters");
        if (parameterValue != null) {
            Map<?, ?> values = map(parameterValue, "parameters");
            values.forEach((key, value) -> {
                if (!(key instanceof String name) || !(value instanceof String text)) {
                    throw new IllegalArgumentException("parameters must contain only string values");
                }
                parameters.put(name, text);
            });
        }
        return new ScenarioStep(id, action, timeout, parameters);
    }

    private static void rejectUnknown(Map<?, ?> source, Set<String> allowed, String label) {
        for (Object key : source.keySet()) {
            if (!(key instanceof String name) || !allowed.contains(name)) {
                throw new IllegalArgumentException("Unknown " + label + " field: " + key);
            }
        }
    }

    private static Map<?, ?> map(Object value, String label) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(label + " must be a mapping");
        }
        return map;
    }

    private static List<String> strings(Object value, String label) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(label + " must be a list");
        }
        return list.stream().map(item -> {
            if (!(item instanceof String text)) {
                throw new IllegalArgumentException(label + " must contain only strings");
            }
            return text;
        }).toList();
    }

    private static Object required(Map<?, ?> source, String name) {
        Object value = source.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + name);
        }
        return value;
    }

    private static String string(Map<?, ?> source, String name) {
        Object value = required(source, name);
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(name + " must be a string");
        }
        return text;
    }

    private static int integer(Map<?, ?> source, String name) {
        Object value = required(source, name);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        return number.intValue();
    }
}
