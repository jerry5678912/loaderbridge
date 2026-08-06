package dev.loaderbridge.agent.dimensions;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class DimensionsDataFixAgentTest {
    @TempDir Path temporaryDirectory;

    @Test
    void activatesOnlyWhenPreparedDimensionsModuleIsInstalled() throws Exception {
        Path mods = Files.createDirectories(temporaryDirectory.resolve("mods"));
        assertThat(DimensionsDataFixAgent.isEnabled(mods)).isFalse();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("LoaderBridge-Dimensions-DataFix", "true");
        try (JarOutputStream jar = new JarOutputStream(
                Files.newOutputStream(mods.resolve("dimensions.jar")), manifest)) {
            jar.flush();
        }
        assertThat(DimensionsDataFixAgent.isEnabled(mods)).isTrue();
    }

    @Test
    void transformsExactPinnedDfuClassesWithoutExternalRuntimeCalls() throws Exception {
        var transformer = new DimensionsDfuTransformer();
        ClassNode tagged = transform(transformer, DimensionsDfuTransformer.TAGGED_CHOICE);
        ClassNode type = transform(transformer, DimensionsDfuTransformer.TAGGED_CHOICE_TYPE);

        assertThat(tagged.fields).extracting(field -> field.name)
                .contains("loaderbridge$dimensionsFailSoft");
        assertThat(tagged.methods).extracting(method -> method.name)
                .contains("loaderbridge$setDimensionsFailSoft");
        assertThat(type.fields).extracting(field -> field.name)
                .contains("loaderbridge$dimensionsFailSoft");
        var codec = type.methods.stream().filter(method -> method.name.equals("getMapCodec"))
                .findFirst().orElseThrow();
        assertThat(java.util.Arrays.stream(codec.instructions.toArray())
                .filter(FieldInsnNode.class::isInstance)
                .map(FieldInsnNode.class::cast)
                .map(field -> field.owner + "." + field.name)
                .toList()).contains("com/mojang/serialization/Codec.PASSTHROUGH");
        assertThat(java.util.Arrays.stream(codec.instructions.toArray())
                .filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast)
                .map(call -> call.name)
                .toList()).contains("containsKey", "assumeMapUnsafe", "success");
    }

    private static ClassNode transform(DimensionsDfuTransformer transformer, String name)
            throws Exception {
        ClassNode sourceNode = new ClassNode(Opcodes.ASM9);
        sourceNode.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, name,
                null, "java/lang/Object", null);
        if (name.equals(DimensionsDfuTransformer.TAGGED_CHOICE)) {
            MethodNode apply = new MethodNode(Opcodes.ACC_PRIVATE, "lambda$apply$0",
                    "(Lcom/mojang/datafixers/util/Pair;)Lcom/mojang/datafixers/types/Type;",
                    null, null);
            apply.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            apply.instructions.add(new InsnNode(Opcodes.ARETURN));
            apply.maxStack = 1;
            apply.maxLocals = 2;
            sourceNode.methods.add(apply);
        } else {
            sourceNode.fields.add(new FieldNode(Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL,
                    "types", "Lit/unimi/dsi/fastutil/objects/Object2ObjectMap;", null, null));
            MethodNode codec = new MethodNode(Opcodes.ACC_PRIVATE, "getMapCodec",
                    "(Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", null, null);
            codec.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            codec.instructions.add(new InsnNode(Opcodes.ARETURN));
            codec.maxStack = 1;
            codec.maxLocals = 2;
            sourceNode.methods.add(codec);
        }
        ClassWriter sourceWriter = new ClassWriter(0);
        sourceNode.accept(sourceWriter);
        byte[] source = sourceWriter.toByteArray();
        byte[] output = transformer.transform(null, null, name, null, null, source);
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(output).accept(node, 0);
        return node;
    }
}
