package dev.loaderbridge.fabric.remap;

import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** Applies version-scoped repairs for callsites structurally moved by Forge patches. */
final class MixinStructuralPatchTransformer {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String AT = "Lorg/spongepowered/asm/mixin/injection/At;";
    private static final Rule LEVEL_BLOCK_NOTIFICATION = new Rule(
            "forge-level-block-notification-v1",
            "net/minecraft/world/level/Level",
            "setBlock(Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            "Lnet/minecraft/world/level/Level;onBlockStateChange("
                    + "Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;)V",
            "Lnet/minecraft/world/level/Level;markAndNotifyBlock("
                    + "Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/chunk/LevelChunk;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;II)V");

    private final List<Rule> rules;

    MixinStructuralPatchTransformer(String minecraftVersion, String forgeVersion) {
        this.rules = minecraftVersion.equals("1.21.1") && forgeVersion.startsWith("52.1.")
                ? List.of(LEVEL_BLOCK_NOTIFICATION) : List.of();
    }

    byte[] transform(byte[] input) {
        if (rules.isEmpty()) return input;
        ClassNode type = new ClassNode();
        new ClassReader(input).accept(type, 0);
        String target = mixinTarget(type.invisibleAnnotations);
        if (target == null) target = mixinTarget(type.visibleAnnotations);
        if (target == null) return input;
        boolean changed = false;
        for (Rule rule : rules) {
            if (!rule.targetClass().equals(target)) continue;
            for (var method : type.methods) {
                changed |= patchAnnotations(method.visibleAnnotations, rule);
                changed |= patchAnnotations(method.invisibleAnnotations, rule);
            }
        }
        if (target.equals("net/minecraft/world/level/chunk/LevelChunk")) {
            for (var method : type.methods) {
                if (replacesMutableShadowMap(type, method)) {
                    changed |= patchConstructorInjection(method.visibleAnnotations);
                    changed |= patchConstructorInjection(method.invisibleAnnotations);
                }
            }
        }
        for (var method : type.methods) {
            if (clearsMutableShadowFields(type, method)) {
                changed |= patchAnyConstructorInjection(method.visibleAnnotations);
                changed |= patchAnyConstructorInjection(method.invisibleAnnotations);
            } else if (constructorStaticHook(method)) {
                changed |= patchAnyConstructorInjection(method.visibleAnnotations);
                changed |= patchAnyConstructorInjection(method.invisibleAnnotations);
            }
        }
        if (!changed) return input;
        ClassWriter writer = new ClassWriter(0);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static String mixinTarget(List<AnnotationNode> annotations) {
        AnnotationNode mixin = annotation(annotations, MIXIN);
        Object targets = value(mixin, "value");
        if (!(targets instanceof List<?> list) || list.size() != 1
                || !(list.getFirst() instanceof Type type)) return null;
        return type.getInternalName();
    }

    private static boolean patchAnnotations(List<AnnotationNode> annotations, Rule rule) {
        AnnotationNode inject = annotation(annotations, INJECT);
        if (inject == null || !contains(value(inject, "method"), rule.injectionMethod())) return false;
        return patchNested(value(inject, "at"), rule);
    }

    private static boolean patchConstructorInjection(List<AnnotationNode> annotations) {
        AnnotationNode inject = annotation(annotations, INJECT);
        if (inject == null || !constructorSelector(value(inject, "method"))) return false;
        Object atValue = value(inject, "at");
        if (!(atValue instanceof List<?> points)) return false;
        boolean changed = false;
        for (Object point : points) {
            if (!(point instanceof AnnotationNode at) || !at.desc.equals(AT)) continue;
            if (!"INVOKE_ASSIGN".equals(value(at, "value"))
                    || !"Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;"
                            .equals(value(at, "target"))) continue;
            setValue(at, "value", "RETURN");
            removeValue(at, "target");
            removeValue(at, "shift");
            changed = true;
        }
        return changed;
    }

    private static boolean patchAnyConstructorInjection(List<AnnotationNode> annotations) {
        AnnotationNode inject = annotation(annotations, INJECT);
        if (inject == null || !constructorSelector(value(inject, "method"))) return false;
        Object atValue = value(inject, "at");
        if (!(atValue instanceof List<?> points)) return false;
        boolean changed = false;
        for (Object point : points) {
            if (!(point instanceof AnnotationNode at) || !at.desc.equals(AT)
                    || "RETURN".equals(value(at, "value"))) continue;
            setValue(at, "value", "RETURN");
            for (String obsolete : List.of(
                    "target", "shift", "by", "args", "ordinal", "opcode", "desc")) {
                removeValue(at, obsolete);
            }
            changed = true;
        }
        return changed;
    }

    private static boolean constructorSelector(Object value) {
        if (value instanceof String text) return text.startsWith("<init>(");
        if (!(value instanceof List<?> list) || list.isEmpty()) return false;
        return list.stream().allMatch(item -> item instanceof String text
                && text.startsWith("<init>("));
    }

    private static boolean replacesMutableShadowMap(ClassNode owner,
            org.objectweb.asm.tree.MethodNode method) {
        FieldInsnNode assignment = null;
        boolean createsFastMap = false;
        for (var instruction = method.instructions.getFirst(); instruction != null;
                instruction = instruction.getNext()) {
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW
                    && type.desc.equals("it/unimi/dsi/fastutil/objects/Object2ObjectOpenHashMap")) {
                createsFastMap = true;
            }
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD
                    && field.owner.equals(owner.name)) {
                if (assignment != null) return false;
                assignment = field;
            }
        }
        if (!createsFastMap || assignment == null) return false;
        String fieldName = assignment.name;
        return owner.fields.stream().anyMatch(field -> field.name.equals(fieldName)
                && annotation(field.visibleAnnotations,
                        "Lorg/spongepowered/asm/mixin/Shadow;") != null
                && annotation(field.visibleAnnotations,
                        "Lorg/spongepowered/asm/mixin/Mutable;") != null);
    }

    private static boolean clearsMutableShadowFields(ClassNode owner,
            org.objectweb.asm.tree.MethodNode method) {
        int writes = 0;
        for (var instruction = method.instructions.getFirst(); instruction != null;
                instruction = instruction.getNext()) {
            int opcode = instruction.getOpcode();
            if (opcode < 0) continue;
            if (opcode == Opcodes.ALOAD) {
                if (!(instruction instanceof org.objectweb.asm.tree.VarInsnNode variable)
                        || variable.var != 0) return false;
            } else if (opcode != Opcodes.ACONST_NULL && opcode != Opcodes.PUTFIELD
                    && opcode != Opcodes.RETURN) {
                return false;
            }
            if (instruction instanceof FieldInsnNode field && opcode == Opcodes.PUTFIELD) {
                if (!field.owner.equals(owner.name) || !mutableShadow(owner, field.name)) return false;
                writes++;
            }
        }
        return writes > 0;
    }

    private static boolean constructorStaticHook(org.objectweb.asm.tree.MethodNode method) {
        int calls = 0;
        for (var instruction = method.instructions.getFirst(); instruction != null;
                instruction = instruction.getNext()) {
            int opcode = instruction.getOpcode();
            if (opcode < 0) continue;
            if (opcode == Opcodes.INVOKESTATIC) {
                calls++;
            } else if (opcode != Opcodes.RETURN) {
                return false;
            }
        }
        return calls > 0;
    }

    private static boolean mutableShadow(ClassNode owner, String fieldName) {
        return owner.fields.stream().anyMatch(field -> field.name.equals(fieldName)
                && annotation(field.visibleAnnotations,
                        "Lorg/spongepowered/asm/mixin/Shadow;") != null
                && annotation(field.visibleAnnotations,
                        "Lorg/spongepowered/asm/mixin/Mutable;") != null);
    }

    private static boolean patchNested(Object value, Rule rule) {
        if (value instanceof AnnotationNode annotation) {
            if (annotation.desc.equals(AT)
                    && rule.oldTarget().equals(value(annotation, "target"))) {
                setValue(annotation, "target", rule.newTarget());
                return true;
            }
            boolean changed = false;
            if (annotation.values != null) {
                for (int index = 1; index < annotation.values.size(); index += 2) {
                    changed |= patchNested(annotation.values.get(index), rule);
                }
            }
            return changed;
        }
        if (!(value instanceof List<?> list)) return false;
        boolean changed = false;
        for (Object nested : list) changed |= patchNested(nested, rule);
        return changed;
    }

    private static boolean contains(Object value, String expected) {
        if (value instanceof String text) return text.equals(expected);
        return value instanceof List<?> list && list.contains(expected);
    }

    private static AnnotationNode annotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) return null;
        return annotations.stream().filter(candidate -> candidate.desc.equals(descriptor))
                .findFirst().orElse(null);
    }

    private static Object value(AnnotationNode annotation, String name) {
        if (annotation == null || annotation.values == null) return null;
        int index = annotation.values.indexOf(name);
        return index < 0 ? null : annotation.values.get(index + 1);
    }

    private static void setValue(AnnotationNode annotation, String name, Object value) {
        int index = annotation.values.indexOf(name);
        annotation.values.set(index + 1, value);
    }

    private static void removeValue(AnnotationNode annotation, String name) {
        int index = annotation.values == null ? -1 : annotation.values.indexOf(name);
        if (index >= 0) {
            annotation.values.remove(index + 1);
            annotation.values.remove(index);
        }
    }

    private record Rule(String id, String targetClass, String injectionMethod,
            String oldTarget, String newTarget) {}
}
