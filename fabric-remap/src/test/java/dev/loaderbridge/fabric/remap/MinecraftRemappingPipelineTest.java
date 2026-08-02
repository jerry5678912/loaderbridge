package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class MinecraftRemappingPipelineTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void remapsFabricInvocationThroughObfuscatedClientHierarchyToOfficialNames() throws Exception {
        Path intermediary = temporaryDirectory.resolve("intermediary.tiny");
        Files.writeString(intermediary, "tiny\t2\t0\tofficial\tintermediary\n"
                + "c\ta\tnet/minecraft/class_1\n"
                + "\tm\t()V\tb\tmethod_1\n");
        Path mojang = temporaryDirectory.resolve("client.txt");
        Files.writeString(mojang, "net.minecraft.Example -> a:\n    void run() -> b\n");
        Path client = temporaryDirectory.resolve("client.jar");
        writeJar(client, "a.class", minecraftClass());
        Path mod = temporaryDirectory.resolve("mod.jar");
        writeJar(mod, "fixture/Caller.class", fabricCaller());
        Path output = temporaryDirectory.resolve("remapped.jar");

        Path runtimeMappings = new MinecraftRemappingPipeline().remap(
                mod, output, client, intermediary, mojang, temporaryDirectory.resolve("work"));

        AtomicReference<String> invocation = new AtomicReference<>();
        try (JarFile jar = new JarFile(output.toFile()); var input = jar.getInputStream(
                jar.getJarEntry("fixture/Caller.class"))) {
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                        String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                                String descriptor, boolean isInterface) {
                            invocation.set(owner + "." + methodName + descriptor);
                        }
                    };
                }
            }, 0);
        }
        assertThat(invocation).hasValue("net/minecraft/Example.run()V");
        assertThat(Files.readString(runtimeMappings))
                .contains("tiny\t2\t0\tintermediary\tnamed")
                .contains("net/minecraft/class_1\tnet/minecraft/Example")
                .contains("method_1\trun");
    }

    private static void writeJar(Path path, String className, byte[] classBytes) throws Exception {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(path))) {
            jar.putNextEntry(new JarEntry(className));
            jar.write(classBytes);
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("{}".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }

    private static byte[] minecraftClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "a", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "b", "()V",
                null, null);
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] fabricCaller() {
        ClassWriter writer = new ClassWriter(0);
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
}
