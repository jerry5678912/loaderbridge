package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/** Validates widened classes and members without defining any target classes. */
final class AccessWidenerTargetValidator {
    private static final long MAXIMUM_CLASS_BYTES = 16L << 20;

    private AccessWidenerTargetValidator() {}

    static void validate(byte[] accessWidener, Path preparedSourceJar, Path minecraftJar)
            throws IOException {
        validate(accessWidener, preparedSourceJar, minecraftJar, null);
    }

    static void validate(byte[] accessWidener, Path preparedSourceJar, Path minecraftJar,
            TinyMappingIndex gameMappings) throws IOException {
        List<Rule> rules = new ArrayList<>();
        new AccessWidenerReader(new RuleCollector(rules)).read(accessWidener, "official");
        try (JarFile source = open(preparedSourceJar); JarFile minecraft = open(minecraftJar)) {
            for (Rule rule : rules) {
                ClassNode target = readClass(source, rule.owner());
                Rule inspectedRule = rule;
                if (target == null) {
                    inspectedRule = gameMappings == null ? rule : rule.toSource(gameMappings);
                    target = readClass(minecraft, inspectedRule.owner());
                }
                if (target == null) {
                    // A mod may legally widen a class supplied by another mod. Those targets are
                    // validated when the full runtime set is scanned; Minecraft targets are fully
                    // knowable during preprocessing and must never be deferred to launch.
                    if (rule.owner().startsWith("net/minecraft/")) {
                        throw new IOException("LB-AW-005: access-widener class target does not exist: "
                                + rule.owner());
                    }
                    continue;
                }
                Rule checked = inspectedRule;
                if (checked.kind() == Kind.FIELD && target.fields.stream().noneMatch(field ->
                        field.name.equals(checked.name()) && field.desc.equals(checked.descriptor()))) {
                    throw missingMember(rule);
                }
                if (checked.kind() == Kind.METHOD && target.methods.stream().noneMatch(method ->
                        method.name.equals(checked.name()) && method.desc.equals(checked.descriptor()))) {
                    throw missingMember(rule);
                }
            }
        }
    }

    private static IOException missingMember(Rule rule) {
        return new IOException("LB-AW-006: access-widener "
                + rule.kind().name().toLowerCase(java.util.Locale.ROOT)
                + " target does not exist: " + rule.owner() + "." + rule.name()
                + " " + rule.descriptor());
    }

    private static JarFile open(Path path) throws IOException {
        return path == null ? null : new JarFile(path.toFile(), false);
    }

    private static ClassNode readClass(JarFile jar, String owner) throws IOException {
        if (jar == null) return null;
        JarEntry entry = jar.getJarEntry(owner + ".class");
        if (entry == null || entry.isDirectory()) return null;
        if (entry.getSize() > MAXIMUM_CLASS_BYTES) {
            throw new IOException("LB-AW-007: access-widener target class exceeds safety limit: "
                    + owner);
        }
        byte[] bytes;
        try (InputStream input = jar.getInputStream(entry)) {
            bytes = input.readNBytes(Math.toIntExact(MAXIMUM_CLASS_BYTES + 1));
        }
        if (bytes.length > MAXIMUM_CLASS_BYTES) {
            throw new IOException("LB-AW-007: access-widener target class exceeds safety limit: "
                    + owner);
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node,
                ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private enum Kind { CLASS, FIELD, METHOD }

    private record Rule(Kind kind, String owner, String name, String descriptor) {
        Rule toSource(TinyMappingIndex mappings) {
            String sourceOwner = mappings.sourceClass(owner);
            if (kind == Kind.CLASS) return new Rule(kind, sourceOwner, null, null);
            String sourceDescriptor = mappings.sourceDescriptor(descriptor);
            String sourceName = kind == Kind.FIELD
                    ? mappings.sourceField(owner, name, descriptor)
                    : mappings.sourceMethod(owner, name, descriptor);
            return new Rule(kind, sourceOwner, sourceName, sourceDescriptor);
        }
    }

    private record RuleCollector(List<Rule> rules) implements AccessWidenerVisitor {
        @Override
        public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
            rules.add(new Rule(Kind.CLASS, name, null, null));
        }

        @Override
        public void visitField(String owner, String name, String descriptor,
                AccessWidenerReader.AccessType access, boolean transitive) {
            rules.add(new Rule(Kind.FIELD, owner, name, descriptor));
        }

        @Override
        public void visitMethod(String owner, String name, String descriptor,
                AccessWidenerReader.AccessType access, boolean transitive) {
            rules.add(new Rule(Kind.METHOD, owner, name, descriptor));
        }
    }
}
