package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class TinyRemapperEngineTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void remapsMethodOwnersAndNamesFromTinyMappings() throws Exception {
        Path input = temporaryDirectory.resolve("input.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(input))) {
            jar.putNextEntry(new JarEntry("fixture/Caller.class"));
            jar.write(callerClass());
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("net/minecraft/class_1.class"));
            jar.write(targetClass());
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("{}".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        Path mappings = temporaryDirectory.resolve("mappings.tiny");
        Files.writeString(mappings, "tiny\t2\t0\tintermediary\tofficial\n"
                + "c\tnet/minecraft/class_1\tnet/minecraft/Example\n"
                + "\tf\tI\tfield_1\targument\n"
                + "\tm\t()V\tmethod_1\trun\n");
        Path output = temporaryDirectory.resolve("output.jar");

        new TinyRemapperEngine().remap(input, output, mappings, "intermediary", "official", List.of());

        AtomicReference<String> invocation = new AtomicReference<>();
        try (JarFile jar = new JarFile(output.toFile()); var stream = jar.getInputStream(
                jar.getJarEntry("fixture/Caller.class"))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                        String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                                String methodDescriptor, boolean isInterface) {
                            invocation.set(owner + "." + methodName + methodDescriptor);
                        }
                    };
                }
            }, 0);
        }
        assertThat(invocation).hasValue("net/minecraft/Example.run()V");
    }

    @Test
    void remapsMixinTargetsAndInjectionMethodSelectors() throws Exception {
        Path input = temporaryDirectory.resolve("mixin-input.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(input))) {
            jar.putNextEntry(new JarEntry("fixture/TargetMixin.class"));
            jar.write(mixinClass());
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("net/minecraft/class_1.class"));
            jar.write(targetClass());
            jar.closeEntry();
        }
        Path mappings = temporaryDirectory.resolve("mixin-mappings.tiny");
        Files.writeString(mappings, "tiny\t2\t0\tintermediary\tofficial\n"
                + "c\tnet/minecraft/class_1\tnet/minecraft/Example\n"
                + "\tf\tI\tfield_1\targument\n"
                + "\tm\t()V\tmethod_1\trun\n");
        Path output = temporaryDirectory.resolve("mixin-output.jar");

        new TinyRemapperEngine().remap(
                input, output, mappings, "intermediary", "official", List.of());

        AtomicReference<String> target = new AtomicReference<>();
        AtomicReference<String> selector = new AtomicReference<>();
        AtomicReference<String> atTarget = new AtomicReference<>();
        AtomicReference<String> accessor = new AtomicReference<>();
        AtomicReference<String> invoker = new AtomicReference<>();
        AtomicReference<String> shadowField = new AtomicReference<>();
        Set<String> remappedMethods = new LinkedHashSet<>();
        Set<String> injectionFamilies = new LinkedHashSet<>();
        try (JarFile jar = new JarFile(output.toFile()); var stream = jar.getInputStream(
                jar.getJarEntry("fixture/TargetMixin.class"))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visit(String ignored, Object value) {
                                    target.set(value.toString());
                                }
                            };
                        }
                    };
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor,
                        String signature, Object value) {
                    return new FieldVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String annotationDescriptor,
                                boolean visible) {
                            if (annotationDescriptor.endsWith("/Shadow;")) shadowField.set(name);
                            return null;
                        }
                    };
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                        String signature, String[] exceptions) {
                    remappedMethods.add(name + descriptor);
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String annotationDescriptor,
                                boolean visible) {
                            String family = annotationDescriptor.substring(
                                    annotationDescriptor.lastIndexOf('/') + 1,
                                    annotationDescriptor.length() - 1);
                            injectionFamilies.add(family);
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visit(String elementName, Object value) {
                                    if (!elementName.equals("value")) return;
                                    if (family.equals("Accessor")) accessor.set(value.toString());
                                    if (family.equals("Invoker")) invoker.set(value.toString());
                                }

                                @Override
                                public AnnotationVisitor visitArray(String elementName) {
                                    if (!elementName.equals("method")) return null;
                                    return new AnnotationVisitor(Opcodes.ASM9) {
                                        @Override
                                        public void visit(String ignored, Object value) {
                                            selector.set(value.toString());
                                        }
                                    };
                                }

                                @Override
                                public AnnotationVisitor visitAnnotation(String elementName,
                                        String descriptor) {
                                    if (!elementName.equals("at")) return null;
                                    return new AnnotationVisitor(Opcodes.ASM9) {
                                        @Override
                                        public void visit(String name, Object value) {
                                            if (name.equals("target")) atTarget.set(value.toString());
                                        }
                                    };
                                }
                            };
                        }
                    };
                }
            }, 0);
        }
        assertThat(target.get()).contains("net/minecraft/Example");
        assertThat(selector).hasValue("run()V");
        assertThat(atTarget).hasValue("Lnet/minecraft/Example;run()V");
        assertThat(injectionFamilies).contains(
                "Inject", "Redirect", "ModifyArg", "ModifyArgs",
                "ModifyVariable", "ModifyConstant");
        assertThat(accessor).hasValue("argument");
        assertThat(invoker).hasValue("run");
        assertThat(shadowField).hasValue("argument");
        assertThat(remappedMethods).contains("run()V");
    }

    private static byte[] callerClass() {
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/Caller", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "call", "()V",
                null, null);
        method.visitCode();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/class_1", "method_1", "()V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] targetClass() {
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "net/minecraft/class_1", null,
                "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PUBLIC, "field_1", "I", null, null).visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "method_1", "()V",
                null, null);
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] mixinClass() {
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "fixture/TargetMixin", null, "java/lang/Object", null);
        AnnotationVisitor mixin = writer.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/Mixin;", false);
        AnnotationVisitor targets = mixin.visitArray("value");
        targets.visit(null, org.objectweb.asm.Type.getObjectType("net/minecraft/class_1"));
        targets.visitEnd();
        mixin.visitEnd();
        FieldVisitor shadow = writer.visitField(Opcodes.ACC_PRIVATE,
                "field_1", "I", null, null);
        shadow.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;", false).visitEnd();
        shadow.visitEnd();
        MethodVisitor overwrite = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "method_1", "()V", null, null);
        overwrite.visitAnnotation("Lorg/spongepowered/asm/mixin/Overwrite;", false).visitEnd();
        overwrite.visitEnd();
        addAccessor(writer, "Accessor", "field_1", "()I");
        addAccessor(writer, "Invoker", "method_1", "()V");
        for (String family : List.of("Inject", "Redirect", "ModifyArg", "ModifyArgs",
                "ModifyVariable", "ModifyConstant")) {
            addInjector(writer, family);
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void addInjector(org.objectweb.asm.ClassWriter writer, String family) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE,
                "handler" + family, "()V", null, null);
        AnnotationVisitor injector = method.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/injection/" + family + ";", false);
        AnnotationVisitor selectors = injector.visitArray("method");
        selectors.visit(null, "method_1");
        selectors.visitEnd();
        AnnotationVisitor at = injector.visitAnnotation(
                "at", "Lorg/spongepowered/asm/mixin/injection/At;");
        at.visit("value", "INVOKE");
        at.visit("target", "Lnet/minecraft/class_1;method_1()V");
        at.visitEnd();
        injector.visitEnd();
        method.visitEnd();
    }

    private static void addAccessor(org.objectweb.asm.ClassWriter writer, String family,
            String target, String descriptor) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "bridge" + family, descriptor, null, null);
        AnnotationVisitor annotation = method.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/gen/" + family + ";", false);
        annotation.visit("value", target);
        annotation.visitEnd();
        method.visitEnd();
    }
}
