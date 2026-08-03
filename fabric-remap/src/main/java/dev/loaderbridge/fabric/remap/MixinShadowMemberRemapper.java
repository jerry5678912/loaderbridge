package dev.loaderbridge.fabric.remap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Repairs shadow members that TinyRemapper cannot resolve on string-targeted nested classes. */
final class MixinShadowMemberRemapper {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private final TinyMappingIndex mappings;

    MixinShadowMemberRemapper(TinyMappingIndex mappings) {
        this.mappings = java.util.Objects.requireNonNull(mappings, "mappings");
    }

    byte[] transform(byte[] input) {
        ClassNode node = new ClassNode();
        new ClassReader(input).accept(node, 0);
        Set<String> targets = new java.util.LinkedHashSet<>();
        collectTargets(node.visibleAnnotations, targets);
        collectTargets(node.invisibleAnnotations, targets);
        if (targets.size() != 1) return input;
        String sourceOwner = mappings.sourceClass(targets.iterator().next());
        Map<Member, String> renamedFields = new LinkedHashMap<>();
        Map<Member, String> renamedMethods = new LinkedHashMap<>();
        for (FieldNode field : node.fields) {
            if (!hasAnnotation(field.visibleAnnotations, SHADOW)
                    && !hasAnnotation(field.invisibleAnnotations, SHADOW)) continue;
            String sourceDescriptor = mappings.sourceDescriptor(field.desc);
            String targetName = mappings.mapField(sourceOwner, field.name, sourceDescriptor);
            if (!targetName.equals(field.name)) {
                renamedFields.put(new Member(field.name, field.desc), targetName);
                field.name = targetName;
            }
        }
        for (MethodNode method : node.methods) {
            if (!hasAnnotation(method.visibleAnnotations, SHADOW)
                    && !hasAnnotation(method.invisibleAnnotations, SHADOW)) continue;
            String sourceDescriptor = mappings.sourceDescriptor(method.desc);
            String targetName = mappings.mapMethod(sourceOwner, method.name, sourceDescriptor);
            if (!targetName.equals(method.name)) {
                renamedMethods.put(new Member(method.name, method.desc), targetName);
                method.name = targetName;
            }
        }
        if (renamedFields.isEmpty() && renamedMethods.isEmpty()) return input;
        for (MethodNode method : node.methods) {
            for (var instruction : method.instructions) {
                if (instruction instanceof FieldInsnNode field && field.owner.equals(node.name)) {
                    field.name = renamedFields.getOrDefault(new Member(field.name, field.desc), field.name);
                } else if (instruction instanceof MethodInsnNode call && call.owner.equals(node.name)) {
                    call.name = renamedMethods.getOrDefault(new Member(call.name, call.desc), call.name);
                }
            }
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void collectTargets(List<AnnotationNode> annotations, Set<String> targets) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            if (!annotation.desc.equals(MIXIN) || annotation.values == null) continue;
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

    private static boolean hasAnnotation(List<AnnotationNode> annotations, String descriptor) {
        return annotations != null && annotations.stream().anyMatch(annotation -> annotation.desc.equals(descriptor));
    }

    private record Member(String name, String descriptor) {}
}
