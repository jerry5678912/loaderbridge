package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
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

    @Test
    void excludesNonRuntimeEntrypointClassesAndTheirNestedClasses() throws Exception {
        Path jarPath = temporaryDirectory.resolve("datagen-references.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            writeReferenceClass(jar, "fixture/Runtime", "net/fabricmc/fabric/api/event/EventFactory");
            writeReferenceClass(jar, "fixture/Datagen",
                    "net/fabricmc/fabric/api/datagen/v1/FabricDataGenerator");
            writeReferenceClass(jar, "fixture/Datagen$Nested",
                    "net/fabricmc/fabric/api/datagen/v1/provider/FabricDynamicRegistryProvider");
            writeReferenceClass(jar, "fixture/datagen/Tags",
                    "net/fabricmc/fabric/api/datagen/v1/provider/FabricTagProvider");
        }

        ReferenceInventory inventory = new BytecodeReferenceAnalyzer().analyze(jarPath,
                Set.of("fixture.Datagen", "fixture.datagen.*"));

        assertThat(inventory.fabricApiClasses())
                .containsExactly("net.fabricmc.fabric.api.event.EventFactory");
    }

    @Test
    void neverTreatsFabricBuildTimeDatagenApiAsRuntimeCapability() throws Exception {
        Path jarPath = temporaryDirectory.resolve("worldgen-provider.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            writeReferenceClass(jar, "fixture/WorldgenProvider",
                    "net/fabricmc/fabric/api/datagen/v1/provider/FabricDynamicRegistryProvider");
        }

        ReferenceInventory inventory = new BytecodeReferenceAnalyzer().analyze(jarPath);

        assertThat(inventory.fabricApiClasses()).isEmpty();
    }

    @Test
    void selectsResourceConditionsBridgeFromStructuredJarContentWithoutJavaReferences()
            throws Exception {
        Path jarPath = temporaryDirectory.resolve("conditional-resources.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("data/fixture/recipe/optional.json"));
            jar.write("""
                    {
                      "type": "missing:serializer",
                      "fabric:load_conditions": [
                        {"condition": "fabric:all_mods_loaded", "values": ["missing"]}
                      ]
                    }
                    """.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        ReferenceInventory inventory = new BytecodeReferenceAnalyzer().analyze(jarPath);

        assertThat(inventory.fabricApiClasses())
                .containsExactly("net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions");
        assertThat(inventory.structuredResourceFeatures())
                .containsExactly("fabric-load-conditions");
    }

    @Test
    void inventoriesConditionalPackOverlaysSeparatelyFromJsonConditions() throws Exception {
        Path jarPath = temporaryDirectory.resolve("conditional-overlay.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("pack.mcmeta"));
            jar.write("{\"pack\":{},\"fabric:overlays\":[]}".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        ReferenceInventory inventory = new BytecodeReferenceAnalyzer().analyze(jarPath);

        assertThat(inventory.structuredResourceFeatures())
                .containsExactly("fabric-conditional-overlays");
        assertThat(inventory.fabricApiClasses()).isEmpty();
    }

    @Test
    void inventoriesMixinFeaturesWhoseLegacyFabricSemanticsDifferFromForgeMixin() throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/LegacyLocalsMixin", null,
                "java/lang/Object", null);
        MethodVisitor modifyVariable = writer.visitMethod(Opcodes.ACC_PRIVATE, "modify",
                "(I)I", null, null);
        modifyVariable.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;", false).visitEnd();
        modifyVariable.visitEnd();
        MethodVisitor inject = writer.visitMethod(Opcodes.ACC_PRIVATE, "capture",
                "()V", null, null);
        AnnotationVisitor annotation = inject.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/injection/Inject;", false);
        annotation.visitEnum("locals",
                "Lorg/spongepowered/asm/mixin/injection/callback/LocalCapture;",
                "CAPTURE_FAILHARD");
        annotation.visitEnd();
        inject.visitEnd();
        writer.visitEnd();

        Path jarPath = temporaryDirectory.resolve("legacy-locals-mixin.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("fixture/LegacyLocalsMixin.class"));
            jar.write(writer.toByteArray());
            jar.closeEntry();
        }

        ReferenceInventory inventory = new BytecodeReferenceAnalyzer().analyze(jarPath);

        assertThat(inventory.mixinSemanticFeatures())
                .containsExactlyInAnyOrder("modify-variable", "inject-local-capture");
    }

    private static void writeReferenceClass(JarOutputStream jar, String className, String owner)
            throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "use", "()V", null, null);
        method.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "reference", "()V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 1);
        method.visitEnd();
        writer.visitEnd();
        jar.putNextEntry(new JarEntry(className + ".class"));
        jar.write(writer.toByteArray());
        jar.closeEntry();
    }
}
