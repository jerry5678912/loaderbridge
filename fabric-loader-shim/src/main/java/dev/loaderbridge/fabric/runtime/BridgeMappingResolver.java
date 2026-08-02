package dev.loaderbridge.fabric.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.MappingResolver;

/** Runtime intermediary-to-Mojang-name mapping resolver backed by Tiny v2 data. */
public final class BridgeMappingResolver implements MappingResolver {
    private static final int MAX_MAPPING_LINES = 500_000;
    private volatile Mappings mappings = Mappings.empty();

    public void install(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return;
        Map<String, String> classes = new LinkedHashMap<>();
        Map<MemberKey, String> fields = new LinkedHashMap<>();
        Map<MemberKey, String> methods = new LinkedHashMap<>();
        String owner = null;
        int lines = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (!"tiny\t2\t0\tintermediary\tnamed".equals(header)) {
                throw new IOException("LB-MAP-001: unsupported runtime mapping header");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (++lines > MAX_MAPPING_LINES) {
                    throw new IOException("LB-MAP-002: runtime mapping exceeds line limit");
                }
                String[] parts = line.split("\t", -1);
                if (parts.length == 3 && parts[0].equals("c")) {
                    owner = parts[1];
                    classes.put(parts[1], parts[2]);
                } else if (parts.length == 5 && parts[0].isEmpty() && owner != null) {
                    MemberKey key = new MemberKey(owner, parts[3], parts[2]);
                    if (parts[1].equals("f")) fields.put(key, parts[4]);
                    else if (parts[1].equals("m")) methods.put(key, parts[4]);
                }
            }
        }
        Map<String, String> reverse = new LinkedHashMap<>();
        classes.forEach((source, target) -> reverse.put(target, source));
        mappings = new Mappings(Map.copyOf(classes), Map.copyOf(reverse),
                Map.copyOf(fields), Map.copyOf(methods));
    }

    @Override public Collection<String> getNamespaces() {
        return mappings.classes().isEmpty()
                ? List.of("named", "official")
                : List.of("intermediary", "named", "official");
    }

    @Override public String getCurrentRuntimeNamespace() { return "named"; }

    @Override
    public String mapClassName(String namespace, String className) {
        validateClassName(className);
        validateNamespace(namespace);
        if (!namespace.equals("intermediary")) return className;
        return mappings.classes().getOrDefault(internal(className), internal(className)).replace('/', '.');
    }

    @Override
    public String unmapClassName(String targetNamespace, String className) {
        validateClassName(className);
        validateNamespace(targetNamespace);
        if (!targetNamespace.equals("intermediary")) return className;
        return mappings.reverseClasses().getOrDefault(internal(className), internal(className))
                .replace('/', '.');
    }

    @Override
    public String mapFieldName(String namespace, String owner, String name, String descriptor) {
        return mapMember(mappings.fields(), namespace, owner, name, descriptor);
    }

    @Override
    public String mapMethodName(String namespace, String owner, String name, String descriptor) {
        return mapMember(mappings.methods(), namespace, owner, name, descriptor);
    }

    private String mapMember(Map<MemberKey, String> values, String namespace, String owner,
            String name, String descriptor) {
        validateClassName(owner);
        validateNamespace(namespace);
        if (!namespace.equals("intermediary")) return name;
        return values.getOrDefault(new MemberKey(internal(owner), name, descriptor), name);
    }

    private void validateNamespace(String namespace) {
        if (!getNamespaces().contains(namespace)) {
            throw new IllegalArgumentException("Unknown mapping namespace: " + namespace);
        }
    }

    private static void validateClassName(String name) {
        if (name.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Class names must be provided in dot format: " + name);
        }
    }

    private static String internal(String binaryName) { return binaryName.replace('.', '/'); }

    private record MemberKey(String owner, String name, String descriptor) {}
    private record Mappings(Map<String, String> classes, Map<String, String> reverseClasses,
            Map<MemberKey, String> fields, Map<MemberKey, String> methods) {
        static Mappings empty() { return new Mappings(Map.of(), Map.of(), Map.of(), Map.of()); }
    }
}
