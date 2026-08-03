package dev.loaderbridge.fabric.remap;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/** Marks selectors translated to the final Forge namespace as ineligible for a second remap. */
final class MixinRuntimeRemapDisabler {
    private static final Set<String> STANDARD = Set.of(
            "Lorg/spongepowered/asm/mixin/Mixin;",
            "Lorg/spongepowered/asm/mixin/Shadow;",
            "Lorg/spongepowered/asm/mixin/Overwrite;",
            "Lorg/spongepowered/asm/mixin/gen/Accessor;",
            "Lorg/spongepowered/asm/mixin/gen/Invoker;",
            "Lorg/spongepowered/asm/mixin/injection/Inject;",
            "Lorg/spongepowered/asm/mixin/injection/Redirect;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArg;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;",
            "Lorg/spongepowered/asm/mixin/injection/At;");
    private static final Set<String> MIXIN_EXTRAS_SIMPLE_NAMES = Set.of(
            "ModifyExpressionValue", "ModifyReceiver", "ModifyReturnValue",
            "WrapWithCondition", "WrapOperation", "WrapMethod");

    byte[] transform(byte[] input) {
        ClassNode type = new ClassNode();
        new ClassReader(input).accept(type, 0);
        boolean changed = disable(type.visibleAnnotations) | disable(type.invisibleAnnotations);
        for (var field : type.fields) {
            changed |= disable(field.visibleAnnotations) | disable(field.invisibleAnnotations);
        }
        for (var method : type.methods) {
            changed |= disable(method.visibleAnnotations) | disable(method.invisibleAnnotations);
            changed |= disableParameters(method.visibleParameterAnnotations);
            changed |= disableParameters(method.invisibleParameterAnnotations);
        }
        if (!changed) return input;
        ClassWriter writer = new ClassWriter(0);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static boolean disableParameters(List<AnnotationNode>[] parameters) {
        if (parameters == null) return false;
        boolean changed = false;
        for (List<AnnotationNode> annotations : parameters) changed |= disable(annotations);
        return changed;
    }

    private static boolean disable(List<AnnotationNode> annotations) {
        if (annotations == null) return false;
        boolean changed = false;
        for (AnnotationNode annotation : annotations) changed |= disable(annotation);
        return changed;
    }

    private static boolean disable(AnnotationNode annotation) {
        boolean changed = false;
        if (supportsRemap(annotation.desc)) {
            if (annotation.values == null) annotation.values = new java.util.ArrayList<>();
            int index = annotation.values.indexOf("remap");
            if (index < 0) {
                annotation.values.add("remap");
                annotation.values.add(Boolean.FALSE);
            } else {
                annotation.values.set(index + 1, Boolean.FALSE);
            }
            changed = true;
        }
        if (annotation.values != null) {
            for (int index = 1; index < annotation.values.size(); index += 2) {
                changed |= disableNested(annotation.values.get(index));
            }
        }
        return changed;
    }

    private static boolean disableNested(Object value) {
        if (value instanceof AnnotationNode nested) return disable(nested);
        if (!(value instanceof List<?> values)) return false;
        boolean changed = false;
        for (Object nested : values) changed |= disableNested(nested);
        return changed;
    }

    private static boolean supportsRemap(String descriptor) {
        if (STANDARD.contains(descriptor)) return true;
        if (!descriptor.startsWith("Lcom/llamalad7/mixinextras/injector/")) return false;
        int separator = descriptor.lastIndexOf('/');
        return MIXIN_EXTRAS_SIMPLE_NAMES.contains(
                descriptor.substring(separator + 1, descriptor.length() - 1));
    }
}
