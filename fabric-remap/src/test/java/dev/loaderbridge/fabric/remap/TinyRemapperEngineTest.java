package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
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
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "method_1", "()V",
                null, null);
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
