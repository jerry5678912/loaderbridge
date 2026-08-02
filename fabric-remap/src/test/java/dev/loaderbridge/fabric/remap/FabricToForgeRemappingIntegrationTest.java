package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.api.BridgeRequest;
import dev.loaderbridge.api.LoaderId;
import java.net.URI;
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
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class FabricToForgeRemappingIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preparesIntermediaryModUsingResolvedMinecraftClasspath() throws Exception {
        Path intermediary = temporaryDirectory.resolve("intermediary.tiny");
        Files.writeString(intermediary, "tiny\t2\t0\tofficial\tintermediary\n"
                + "c\ta\tnet/minecraft/class_1\n\tm\t()V\tb\tmethod_1\n");
        Path officialMappings = temporaryDirectory.resolve("client.txt");
        Files.writeString(officialMappings, "net.minecraft.Example -> a:\n    void run() -> b\n");
        Path client = temporaryDirectory.resolve("client.jar");
        writeJar(client, "a.class", minecraftClass(), null);
        Path mod = temporaryDirectory.resolve("fabric-mod.jar");
        writeJar(mod, "fixture/Caller.class", fabricCaller(),
                "{\"schemaVersion\":1,\"id\":\"fixture\",\"version\":\"1.0.0\"}");
        ResolvedMinecraftArtifacts resolved = new ResolvedMinecraftArtifacts("1.21.1",
                artifact("client", client), artifact("client_mappings", officialMappings));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter(
                (version, cache, refresh) -> resolved, (version, cache) -> intermediary);
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(mod), temporaryDirectory.resolve("output"),
                temporaryDirectory.resolve("cache"));

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(result.succeeded()).isTrue();
        AtomicReference<String> invocation = invocation(result.artifacts().getFirst());
        assertThat(invocation).hasValue("net/minecraft/Example.run()V");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("client_mappings", "https://example.invalid/client_mappings");
    }

    private static ResolvedArtifact artifact(String id, Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        return new ResolvedArtifact(id, URI.create("https://example.invalid/" + id),
                Integer.toHexString(java.util.Arrays.hashCode(bytes)), bytes.length, path);
    }

    private static AtomicReference<String> invocation(Path jarPath) throws Exception {
        AtomicReference<String> result = new AtomicReference<>();
        try (JarFile jar = new JarFile(jarPath.toFile()); var input = jar.getInputStream(
                jar.getJarEntry("fixture/Caller.class"))) {
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                        String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                                String descriptor, boolean isInterface) {
                            result.set(owner + "." + methodName + descriptor);
                        }
                    };
                }
            }, 0);
        }
        return result;
    }

    private static void writeJar(Path path, String className, byte[] classBytes, String metadata)
            throws Exception {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(path))) {
            jar.putNextEntry(new JarEntry(className));
            jar.write(classBytes);
            jar.closeEntry();
            if (metadata != null) {
                jar.putNextEntry(new JarEntry("fabric.mod.json"));
                jar.write(metadata.getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
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
