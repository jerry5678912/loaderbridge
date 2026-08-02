package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.api.BridgeRequest;
import dev.loaderbridge.api.LoaderId;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class RealMinecraftRemappingLiveTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "LOADERBRIDGE_LIVE", matches = "true")
    void remapsControlledFabricFixtureAgainstRealMinecraft1211() throws Exception {
        Path root = Path.of("build/live-remapping-probe").toAbsolutePath();
        Files.createDirectories(root);
        Path mod = root.resolve("fixture-fabric.jar");
        writeFixture(mod);
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(mod), root.resolve("output"),
                Path.of("build/live-minecraft-cache").toAbsolutePath());
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var prepared = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).as(plan.diagnostics().toString()).isTrue();
        assertThat(prepared.succeeded()).isTrue();
        assertThat(invocation(prepared.artifacts().getFirst())).hasValue(
                "net/minecraft/client/Minecraft.getInstance()Lnet/minecraft/client/Minecraft;");
    }

    private static void writeFixture(Path output) throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/RealMinecraftCaller", null,
                "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "minecraft", "()V",
                null, null);
        method.visitCode();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/class_310", "method_1551",
                "()Lnet/minecraft/class_310;", false);
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(output))) {
            jar.putNextEntry(new JarEntry("fixture/RealMinecraftCaller.class"));
            jar.write(writer.toByteArray());
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("{\"schemaVersion\":1,\"id\":\"real_probe\",\"version\":\"1.0.0\"}"
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }

    private static AtomicReference<String> invocation(Path output) throws Exception {
        AtomicReference<String> invocation = new AtomicReference<>();
        try (JarFile jar = new JarFile(output.toFile()); var input = jar.getInputStream(
                jar.getJarEntry("fixture/RealMinecraftCaller.class"))) {
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
        return invocation;
    }
}
