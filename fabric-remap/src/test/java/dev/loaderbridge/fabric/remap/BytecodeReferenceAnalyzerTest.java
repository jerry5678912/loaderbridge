package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class BytecodeReferenceAnalyzerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void inventoriesFabricApiAndNativeLibrariesWithoutLoadingClasses() throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/Example", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "use", "()V", null, null);
        AnnotationVisitor mixinExtras = method.visitAnnotation(
                "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;", false);
        mixinExtras.visitEnd();
        method.visitCode();
        method.visitMethodInsn(Opcodes.INVOKESTATIC,
                "net/fabricmc/fabric/api/event/EventFactory", "createArrayBacked", "()V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 1);
        method.visitEnd();
        writer.visitEnd();

        Path jarPath = temporaryDirectory.resolve("references.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("fixture/Example.class"));
            jar.write(writer.toByteArray());
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/natives/linux/libfixture.so"));
            jar.write(new byte[] {1});
            jar.closeEntry();
        }

        ReferenceInventory inventory = new BytecodeReferenceAnalyzer().analyze(jarPath);

        assertThat(inventory.fabricApiClasses())
                .contains("net.fabricmc.fabric.api.event.EventFactory");
        assertThat(inventory.mixinExtrasClasses())
                .contains("com.llamalad7.mixinextras.injector.ModifyReturnValue");
        assertThat(inventory.nativeLibraries()).contains("META-INF/natives/linux/libfixture.so");
    }
}
