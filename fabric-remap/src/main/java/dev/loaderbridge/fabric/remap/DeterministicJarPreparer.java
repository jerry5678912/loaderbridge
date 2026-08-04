package dev.loaderbridge.fabric.remap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loaderbridge.fabric.metadata.FabricModMetadata;
import dev.loaderbridge.fabric.metadata.UnsafeJarException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/** Produces the deterministic packaging envelope around transformed bytecode. */
public final class DeterministicJarPreparer {
    private static final long FIXED_ENTRY_TIME = 0L;

    public void prepare(
            Path source,
            Path destination,
            FabricModMetadata metadata,
            PreparationManifest manifest) throws IOException {
        prepare(source, destination, metadata, manifest, null);
    }

    public void prepare(
            Path source,
            Path destination,
            FabricModMetadata metadata,
            PreparationManifest manifest,
            Path runtimeMappings) throws IOException {
        prepare(source, destination, metadata, manifest, runtimeMappings, null);
    }

    public void prepare(
            Path source,
            Path destination,
            FabricModMetadata metadata,
            PreparationManifest manifest,
            Path runtimeMappings,
            Path targetGameJar) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent == null) {
            throw new IOException("Output JAR must have a parent directory");
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, destination.getFileName().toString(), ".tmp");
        try {
            write(source, temporary, metadata, manifest, runtimeMappings, targetGameJar);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }
    }

    private static void write(
            Path source,
            Path destination,
            FabricModMetadata metadata,
            PreparationManifest manifest,
            Path runtimeMappings,
            Path targetGameJar) throws IOException {
        try (JarFile input = new JarFile(source.toFile(), false);
                JarOutputStream output = new JarOutputStream(Files.newOutputStream(destination))) {
            Set<String> names = new HashSet<>();
            boolean augmentManifest = !metadata.mixins().isEmpty() || metadata.accessWidener().isPresent();
            MixinPackaging mixins = prepareMixins(
                    input, metadata, manifest.sourceNamespace(), runtimeMappings);
            AccessWidenerPackaging accessWidener = prepareAccessWidener(
                    input, source, metadata, manifest.sourceNamespace(), runtimeMappings,
                    targetGameJar);
            TinyMappingIndex bytecodeMappings = manifest.sourceNamespace().equals("intermediary")
                    && runtimeMappings != null ? TinyMappingIndex.read(runtimeMappings) : null;
            if (augmentManifest) {
                put(output, "META-INF/MANIFEST.MF", bridgeManifest(
                        input, mixins.configs(), accessWidener.resource()));
                names.add("META-INF/MANIFEST.MF");
            }
            List<JarEntry> entries = input.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> !augmentManifest
                            || !entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF"))
                    .filter(entry -> !isInvalidatedSignature(entry.getName()))
                    .filter(entry -> !isGeneratedEntry(entry.getName()))
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .toList();
            for (JarEntry entry : entries) {
                validateName(entry.getName());
                if (!names.add(entry.getName())) {
                    throw new UnsafeJarException("Duplicate JAR entry: " + entry.getName());
                }
                try (InputStream bytes = input.getInputStream(entry)) {
                    byte[] content = readBounded(bytes);
                    if (manifest.targetNamespace().equals("official")
                            && entry.getName().endsWith(".class")) {
                        content = new MixinStructuralPatchTransformer(
                                manifest.minecraftVersion(), manifest.forgeVersion()).transform(content);
                        if (bytecodeMappings != null) {
                            content = new MixinShadowMemberRemapper(bytecodeMappings).transform(content);
                        }
                        content = new MixinRuntimeRemapDisabler().transform(content);
                    }
                    put(output, entry.getName(), content);
                }
            }
            if (!names.contains("pack.mcmeta")) {
                put(output, "pack.mcmeta", packMetadata(metadata));
            }
            if (runtimeMappings != null) {
                try (InputStream bytes = Files.newInputStream(runtimeMappings)) {
                    put(output, "META-INF/loaderbridge/mappings.tiny",
                            runtimeMappingsForForge(readBounded(bytes)));
                }
            }
            for (Map.Entry<String, byte[]> generated : mixins.generatedResources().entrySet()) {
                put(output, generated.getKey(), generated.getValue());
            }
            if (accessWidener.resource() != null) {
                put(output, accessWidener.resource(), accessWidener.bytes());
            }
            put(output, "META-INF/loaderbridge.json", bridgeMetadata(metadata, manifest));
            put(output, "META-INF/mods.toml", forgeMetadata(
                    metadata, manifest.fulfilledFabricDependencies().keySet(),
                    manifest.resolvedDependencyModIds()));
        }
    }

    private static byte[] runtimeMappingsForForge(byte[] mappings) throws IOException {
        String content = new String(mappings, StandardCharsets.UTF_8);
        String internalHeader = "tiny\t2\t0\tintermediary\tnamed\n";
        String runtimeHeader = "tiny\t2\t0\tintermediary\tofficial\n";
        if (content.startsWith(runtimeHeader)) return mappings;
        if (!content.startsWith(internalHeader)) {
            throw new IOException("LB-MAP-001: unsupported runtime mapping header");
        }
        return (runtimeHeader + content.substring(internalHeader.length()))
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] bridgeMetadata(FabricModMetadata metadata, PreparationManifest manifest) {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", manifest.formatVersion());
        root.addProperty("adapter", "fabric-to-forge");
        root.addProperty("adapterVersion", manifest.adapterVersion());
        root.addProperty("sourceLoader", "fabric");
        root.addProperty("targetLoader", "forge");
        root.addProperty("minecraftVersion", manifest.minecraftVersion());
        root.addProperty("forgeVersion", manifest.forgeVersion());
        root.addProperty("sourceNamespace", manifest.sourceNamespace());
        root.addProperty("targetNamespace", manifest.targetNamespace());
        root.addProperty("modId", metadata.id());
        root.addProperty("modVersion", metadata.version());
        if (!manifest.fulfilledFabricDependencies().isEmpty()) {
            root.add("fulfilledFabricDependencies", new GsonBuilder().create().toJsonTree(
                    manifest.fulfilledFabricDependencies()));
        }
        if (!manifest.resolvedDependencyModIds().isEmpty()) {
            root.add("resolvedDependencyModIds", new GsonBuilder().create().toJsonTree(
                    manifest.resolvedDependencyModIds()));
        }
        if (manifest.parentModId() != null) {
            root.addProperty("parentModId", manifest.parentModId());
            root.addProperty("parentSubLocation", manifest.parentSubLocation());
        }
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] forgeMetadata(FabricModMetadata metadata,
            Set<String> fulfilledFabricDependencies,
            Map<String, String> resolvedDependencyModIds) {
        String escapedId = toml(hostModId(metadata.id()));
        String escapedVersion = toml(metadata.version());
        String escapedName = toml(metadata.name());
        String text = "modLoader=\"fabricbridge\"\n"
                + "loaderVersion=\"[0.1,1)\"\n"
                + "license=\"LicenseRef-See-fabric.mod.json\"\n\n"
                + "[[mods]]\n"
                + "modId=\"" + escapedId + "\"\n"
                + "version=\"" + escapedVersion + "\"\n"
                + "displayName=\"" + escapedName + "\"\n";
        StringBuilder result = new StringBuilder(text);
        appendDependencies(result, escapedId, metadata.dependencies().depends(), true,
                fulfilledFabricDependencies, resolvedDependencyModIds);
        appendDependencies(result, escapedId, metadata.dependencies().recommends(), false,
                fulfilledFabricDependencies, resolvedDependencyModIds);
        appendDependencies(result, escapedId, metadata.dependencies().suggests(), false,
                fulfilledFabricDependencies, resolvedDependencyModIds);
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] packMetadata(FabricModMetadata metadata) {
        JsonObject pack = new JsonObject();
        pack.addProperty("description", metadata.name() + " resources");
        pack.addProperty("pack_format", 34);
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    private static MixinPackaging prepareMixins(JarFile input, FabricModMetadata metadata,
            String sourceNamespace, Path runtimeMappings)
            throws IOException {
        List<String> configs = new ArrayList<>();
        Map<String, byte[]> generated = new java.util.TreeMap<>();
        TinyMappingIndex mappingIndex = sourceNamespace.equals("intermediary")
                && runtimeMappings != null ? TinyMappingIndex.read(runtimeMappings) : null;
        Map<String, String> allMixinTargetOwners = new LinkedHashMap<>();
        if (mappingIndex != null) {
            for (var declaredMixin : metadata.mixins()) {
                validateMixinConfig(declaredMixin.config());
                allMixinTargetOwners.putAll(mixinTargetOwners(
                        input, readMixinConfig(input, declaredMixin.config())));
            }
        }
        for (var mixin : metadata.mixins()) {
            validateMixinConfig(mixin.config());
            JsonObject config = readMixinConfig(input, mixin.config());
            boolean generatedConfig = false;
            if (!mixin.environment().equals("*") && !mixin.environment().equals("client")
                    && !mixin.environment().equals("server")) {
                throw new UnsafeJarException("Unsupported Mixin environment: "
                        + mixin.environment());
            }
            if (!mixin.environment().equals("*")) {
                JsonArray sideMixins = new JsonArray();
                appendMixinNames(config.get("mixins"), sideMixins, mixin.config());
                appendMixinNames(config.get(mixin.environment()), sideMixins, mixin.config());
                config.remove("mixins");
                config.remove("client");
                config.remove("server");
                config.add(mixin.environment(), sideMixins);
                generatedConfig = true;
            }
            if (mappingIndex != null && config.has("refmap")) {
                JsonElement refmapValue = config.get("refmap");
                if (!refmapValue.isJsonPrimitive()
                        || !refmapValue.getAsJsonPrimitive().isString()) {
                    throw new UnsafeJarException(
                            "LB-MIXIN-003: refmap must be a resource string: " + mixin.config());
                }
                String refmap = refmapValue.getAsString();
                validateMixinConfig(refmap);
                byte[] translated = new MixinRefmapTransformer().transform(
                        readResource(input, refmap, "LB-MIXIN-REFMAP-004"), mappingIndex, refmap,
                        allMixinTargetOwners);
                String translatedName = "META-INF/loaderbridge/mixins/"
                        + sha256("refmap\u0000" + refmap) + ".refmap.json";
                generated.put(translatedName, translated);
                config.addProperty("refmap", translatedName);
                generatedConfig = true;
            }
            if (!generatedConfig) {
                configs.add(mixin.config());
                continue;
            }
            String generatedName = "META-INF/loaderbridge/mixins/"
                    + sha256(mixin.environment() + "\u0000" + mixin.config()) + ".json";
            generated.put(generatedName,
                    new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                            .toJson(config).getBytes(StandardCharsets.UTF_8));
            configs.add(generatedName);
        }
        return new MixinPackaging(configs.stream().distinct().sorted().toList(),
                java.util.Collections.unmodifiableMap(new LinkedHashMap<>(generated)));
    }

    static Map<String, String> mixinTargetOwners(JarFile input, JsonObject config)
            throws IOException {
        String packageName = config.has("package") ? config.get("package").getAsString() : "";
        Set<String> names = new java.util.LinkedHashSet<>();
        collectMixinNames(config.get("mixins"), names);
        collectMixinNames(config.get("client"), names);
        collectMixinNames(config.get("server"), names);
        Map<String, String> owners = new LinkedHashMap<>();
        for (String name : names) {
            String binaryName = packageName.isEmpty() || name.startsWith(packageName + ".")
                    ? name : packageName + "." + name;
            JarEntry entry = input.getJarEntry(binaryName.replace('.', '/') + ".class");
            if (entry == null) continue;
            ClassNode node = new ClassNode();
            try (InputStream stream = input.getInputStream(entry)) {
                new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG
                        | ClassReader.SKIP_FRAMES);
            }
            Set<String> targets = new java.util.LinkedHashSet<>();
            collectMixinTargets(node.visibleAnnotations, targets);
            collectMixinTargets(node.invisibleAnnotations, targets);
            if (targets.size() == 1) {
                String owner = targets.iterator().next();
                owners.put(binaryName, owner);
                owners.put(binaryName.replace('.', '/'), owner);
            }
        }
        return Map.copyOf(owners);
    }

    private static void collectMixinNames(JsonElement value, Set<String> names)
            throws UnsafeJarException {
        if (value == null) return;
        if (!value.isJsonArray()) throw new UnsafeJarException("LB-MIXIN-003: Mixin list must be an array");
        for (JsonElement element : value.getAsJsonArray()) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                names.add(element.getAsString());
            }
        }
    }

    private static void collectMixinTargets(List<AnnotationNode> annotations, Set<String> targets) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")
                    || annotation.values == null) continue;
            for (int index = 0; index < annotation.values.size(); index += 2) {
                String key = (String) annotation.values.get(index);
                Object value = annotation.values.get(index + 1);
                if (key.equals("value") && value instanceof List<?> values) {
                    values.stream().filter(Type.class::isInstance).map(Type.class::cast)
                            .map(Type::getInternalName).forEach(targets::add);
                } else if (key.equals("targets") && value instanceof List<?> values) {
                    values.stream().filter(String.class::isInstance).map(String.class::cast)
                            .map(target -> target.replace('.', '/')).forEach(targets::add);
                }
            }
        }
    }

    private static JsonObject readMixinConfig(JarFile input, String name) throws IOException {
        byte[] bytes = readResource(input, name, "LB-MIXIN-002");
        try {
            JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new UnsafeJarException("LB-MIXIN-003: Mixin config must be an object: " + name);
            }
            return parsed.getAsJsonObject().deepCopy();
        } catch (com.google.gson.JsonParseException exception) {
            UnsafeJarException failure = new UnsafeJarException(
                    "LB-MIXIN-003: malformed Mixin config: " + name);
            failure.initCause(exception);
            throw failure;
        }
    }

    private static byte[] readResource(JarFile input, String name, String code) throws IOException {
        JarEntry entry = input.getJarEntry(name);
        if (entry == null || entry.isDirectory()) {
            throw new UnsafeJarException(code + ": missing resource: " + name);
        }
        try (InputStream inputStream = input.getInputStream(entry)) {
            return readBounded(inputStream);
        }
    }

    private static void appendMixinNames(JsonElement value, JsonArray destination, String config)
            throws UnsafeJarException {
        if (value == null) return;
        if (!value.isJsonArray()) {
            throw new UnsafeJarException("LB-MIXIN-003: Mixin list must be an array: " + config);
        }
        for (JsonElement element : value.getAsJsonArray()) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new UnsafeJarException("LB-MIXIN-003: Mixin name must be a string: " + config);
            }
            destination.add(element.getAsString());
        }
    }

    private static AccessWidenerPackaging prepareAccessWidener(JarFile input, Path source,
            FabricModMetadata metadata, String sourceNamespace, Path runtimeMappings,
            Path targetGameJar) throws IOException {
        if (metadata.accessWidener().isEmpty()) {
            return new AccessWidenerPackaging(null, null);
        }
        String original = metadata.accessWidener().orElseThrow();
        validateResourceName(original, "access widener");
        byte[] bytes = readResource(input, original, "LB-AW-004");
        // Access-widener resources declare their own namespace. Metadata-only modules have no
        // bytecode from which to infer a source namespace, but their intermediary rules still
        // need the resolved runtime mappings.
        TinyMappingIndex mappings = runtimeMappings == null
                ? null : TinyMappingIndex.read(runtimeMappings);
        byte[] transformed = new AccessWidenerResourceTransformer().transform(bytes, mappings);
        if (targetGameJar != null) {
            AccessWidenerTargetValidator.validate(transformed, source, targetGameJar, mappings);
        }
        String generated = "META-INF/loaderbridge/access-wideners/"
                + sha256("access-widener\u0000" + original) + ".accesswidener";
        return new AccessWidenerPackaging(generated, transformed);
    }

    private static byte[] bridgeManifest(JarFile input, List<String> configs,
            String accessWidener)
            throws IOException {
        Manifest manifest = new Manifest();
        JarEntry existing = input.getJarEntry("META-INF/MANIFEST.MF");
        if (existing != null) {
            try (InputStream bytes = input.getInputStream(existing)) {
                manifest.read(new ByteArrayInputStream(readBounded(bytes)));
            }
        }
        if (manifest.getMainAttributes().getValue("Manifest-Version") == null) {
            manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        }
        if (!configs.isEmpty()) {
            manifest.getMainAttributes().putValue("MixinConfigs", String.join(",", configs));
        }
        if (accessWidener != null) {
            manifest.getMainAttributes().putValue("LoaderBridge-Access-Widener", accessWidener);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        manifest.write(output);
        return output.toByteArray();
    }

    private static void validateMixinConfig(String config) throws UnsafeJarException {
        validateResourceName(config, "Mixin configuration");
    }

    private static void validateResourceName(String config, String kind) throws UnsafeJarException {
        if (config.isBlank() || config.startsWith("/") || config.contains("\\")
                || config.contains("\t") || config.contains("\r") || config.contains("\n")
                || List.of(config.split("/")).contains("..")) {
            throw new UnsafeJarException("Unsafe " + kind + " resource: " + config);
        }
    }

    private static void appendDependencies(StringBuilder target, String owner,
            Map<String, List<String>> dependencies, boolean mandatory,
            Set<String> fulfilledFabricDependencies,
            Map<String, String> resolvedDependencyModIds) {
        dependencies.keySet().stream()
                .filter(id -> !Set.of("minecraft", "java", "fabricloader").contains(id))
                .filter(id -> !fulfilledFabricDependencies.contains(id))
                .sorted()
                .forEach(id -> target.append("\n[[dependencies.").append(owner).append("]]\n")
                        .append("modId=\"").append(toml(hostModId(
                                resolvedDependencyModIds.getOrDefault(id, id)))).append("\"\n")
                        .append("mandatory=").append(mandatory).append("\n")
                        .append("versionRange=\"[0,)\"\n")
                        .append("ordering=\"AFTER\"\n")
                        .append("side=\"BOTH\"\n"));
    }

    static String hostModId(String fabricId) {
        if (fabricId.matches("^[a-z][a-z0-9_]{1,63}$")) {
            return fabricId;
        }
        String sanitized = fabricId.replaceAll("[^a-z0-9_]", "_");
        if (sanitized.isEmpty() || !Character.isLowerCase(sanitized.charAt(0))) {
            sanitized = "m_" + sanitized;
        }
        String suffix = "_" + shortHash(fabricId);
        int maxBaseLength = 64 - suffix.length();
        if (sanitized.length() > maxBaseLength) {
            sanitized = sanitized.substring(0, maxBaseLength);
        }
        return sanitized + suffix;
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 4);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java", exception);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java", exception);
        }
    }

    private static String toml(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        long total = 0;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > (64L << 20)) {
                throw new UnsafeJarException("JAR entry exceeds preprocessing size limit");
            }
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static void put(JarOutputStream output, String name, byte[] bytes) throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setTime(FIXED_ENTRY_TIME);
        output.putNextEntry(entry);
        output.write(bytes);
        output.closeEntry();
    }

    private static boolean isGeneratedEntry(String name) {
        return name.equals("META-INF/mods.toml") || name.equals("META-INF/loaderbridge.json")
                || name.equals("META-INF/loaderbridge/mappings.tiny")
                || name.startsWith("META-INF/loaderbridge/mixins/")
                || name.startsWith("META-INF/loaderbridge/access-wideners/");
    }

    private static boolean isInvalidatedSignature(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) {
            return false;
        }
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA")
                || upper.endsWith(".EC") || upper.equals("META-INF/INDEX.LIST");
    }

    private static void validateName(String name) throws UnsafeJarException {
        if (name.isBlank() || name.startsWith("/") || name.contains("\\")) {
            throw new UnsafeJarException("Unsafe JAR entry: " + name);
        }
        List<String> parts = new ArrayList<>(List.of(name.split("/")));
        if (parts.contains("..") || parts.contains(".")) {
            throw new UnsafeJarException("Unsafe JAR entry: " + name);
        }
    }

    private record MixinPackaging(List<String> configs, Map<String, byte[]> generatedResources) {}

    private record AccessWidenerPackaging(String resource, byte[] bytes) {}
}
