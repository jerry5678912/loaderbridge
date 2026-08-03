package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class MixinRuntimeRemapDisablerTest {
    @Test
    void disablesSecondRemapForTargetsShadowsInjectorsNestedAtAndMixinExtras() {
        byte[] output = new MixinRuntimeRemapDisabler().transform(mixinClass());
        Map<String, Boolean> remap = new LinkedHashMap<>();
        new ClassReader(output).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                return capture(descriptor, remap);
            }

            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name,
                    String descriptor, String signature, Object value) {
                return new org.objectweb.asm.FieldVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        return capture(annotation, remap);
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        return capture(annotation, remap);
                    }
                };
            }
        }, 0);

        assertThat(remap).containsEntry("Mixin", false)
                .containsEntry("Shadow", false)
                .containsEntry("Inject", false)
                .containsEntry("At", false)
                .containsEntry("ModifyReturnValue", false);
    }

    private static AnnotationVisitor capture(String descriptor, Map<String, Boolean> values) {
        String name = descriptor.substring(descriptor.lastIndexOf('/') + 1,
                descriptor.length() - 1);
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String element, Object value) {
                if (element.equals("remap")) values.put(name, (Boolean) value);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String element, String nestedDescriptor) {
                return capture(nestedDescriptor, values);
            }

            @Override
            public AnnotationVisitor visitArray(String element) {
                return this;
            }
        };
    }

    private static byte[] mixinClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/Mixin", null,
                "java/lang/Object", null);
        AnnotationVisitor mixin = writer.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/Mixin;", false);
        mixin.visit("remap", true);
        mixin.visitEnd();
        writer.visitField(Opcodes.ACC_PRIVATE, "f_1_", "I", null, null)
                .visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;", false).visitEnd();
        MethodVisitor inject = writer.visitMethod(Opcodes.ACC_PRIVATE, "inject", "()V", null, null);
        AnnotationVisitor injector = inject.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/injection/Inject;", false);
        AnnotationVisitor at = injector.visitAnnotation(
                "at", "Lorg/spongepowered/asm/mixin/injection/At;");
        at.visit("value", "HEAD");
        at.visitEnd();
        injector.visitEnd();
        inject.visitEnd();
        writer.visitMethod(Opcodes.ACC_PRIVATE, "extra", "()V", null, null)
                .visitAnnotation("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;", false)
                .visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
