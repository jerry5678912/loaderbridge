package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class AccessWidenerTargetValidatorTest {
    @TempDir Path temporaryDirectory;

    @Test
    void acceptsExistingMinecraftClassFieldAndMethodTargets() throws Exception {
        Path gameJar = gameJar("net/minecraft/Example", "value", "run");
        byte[] widener = widener("""
                accessible class net/minecraft/Example
                mutable field net/minecraft/Example value I
                extendable method net/minecraft/Example run ()V
                """);

        assertThatCode(() -> AccessWidenerTargetValidator.validate(widener, null, gameJar))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingMinecraftClassesWithStableDiagnostic() throws Exception {
        Path gameJar = gameJar("net/minecraft/Example", "value", "run");

        assertThatThrownBy(() -> AccessWidenerTargetValidator.validate(widener("""
                accessible class net/minecraft/Missing
                """), null, gameJar))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("LB-AW-005")
                .hasMessageContaining("net/minecraft/Missing");
    }

    @Test
    void rejectsMissingMinecraftMembersWithStableDiagnostic() throws Exception {
        Path gameJar = gameJar("net/minecraft/Example", "value", "run");

        assertThatThrownBy(() -> AccessWidenerTargetValidator.validate(widener("""
                accessible field net/minecraft/Example absent I
                """), null, gameJar))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("LB-AW-006")
                .hasMessageContaining("net/minecraft/Example.absent I");
    }

    @Test
    void validatesModOwnedTargetsAgainstThePreparedSourceJar() throws Exception {
        Path sourceJar = gameJar("example/mod/Internal", "state", "tick");
        Path gameJar = gameJar("net/minecraft/Example", "value", "run");

        assertThatCode(() -> AccessWidenerTargetValidator.validate(widener("""
                accessible field example/mod/Internal state I
                accessible method example/mod/Internal tick ()V
                """), sourceJar, gameJar)).doesNotThrowAnyException();
    }

    @Test
    void validatesOfficialRulesAgainstAnIntermediaryMinecraftJar() throws Exception {
        Path mappingsFile = temporaryDirectory.resolve("mappings.tiny");
        Files.writeString(mappingsFile, "tiny\t2\t0\tintermediary\tnamed\n"
                + "c\tnet/minecraft/class_1\tnet/minecraft/Example\n"
                + "\tf\tI\tfield_1\tvalue\n"
                + "\tm\t(Lnet/minecraft/class_1;)V\tmethod_1\trun\n");
        Path intermediaryGame = gameJar(
                "net/minecraft/class_1", "field_1", "method_1",
                "(Lnet/minecraft/class_1;)V");

        assertThatCode(() -> AccessWidenerTargetValidator.validate(widener("""
                accessible class net/minecraft/Example
                mutable field net/minecraft/Example value I
                extendable method net/minecraft/Example run (Lnet/minecraft/Example;)V
                accessible method net/minecraft/Example <init> ()V
                """), null, intermediaryGame, TinyMappingIndex.read(mappingsFile)))
                .doesNotThrowAnyException();
    }

    private byte[] widener(String rules) {
        return ("accessWidener v2 official\n" + rules).getBytes(StandardCharsets.UTF_8);
    }

    private Path gameJar(String className, String fieldName, String methodName) throws Exception {
        return gameJar(className, fieldName, methodName, "()V");
    }

    private Path gameJar(String className, String fieldName, String methodName,
            String methodDescriptor) throws Exception {
        Path output = temporaryDirectory.resolve(className.replace('/', '-') + ".jar");
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, "I", null, null).visitEnd();
        MethodVisitor constructor = writer.visitMethod(
                Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, methodName, methodDescriptor, null, null);
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 1);
        method.visitEnd();
        writer.visitEnd();
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(output))) {
            jar.putNextEntry(new JarEntry(className + ".class"));
            jar.write(writer.toByteArray());
            jar.closeEntry();
        }
        return output;
    }
}
