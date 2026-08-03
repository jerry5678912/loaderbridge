package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;

class MixinShadowMemberRemapperTest {
    @TempDir Path temporaryDirectory;

    @Test
    void renamesShadowAndItsSelfReferencesUsingTheOfficialTargetOwner() throws Exception {
        Path mappingFile = temporaryDirectory.resolve("mappings.tiny");
        Files.writeString(mappingFile, "tiny\t2\t0\tintermediary\tnamed\n"
                + "c\tnet/minecraft/class_1\tnet/minecraft/Target\n"
                + "\tf\tLnet/minecraft/class_2;\tfield_1\tvalue\n"
                + "c\tnet/minecraft/class_2\tnet/minecraft/Value\n");
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "fixture/TargetMixin", null,
                "java/lang/Object", null);
        var mixin = writer.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;", false);
        var targets = mixin.visitArray("value");
        targets.visit(null, Type.getObjectType("net/minecraft/Target"));
        targets.visitEnd();
        mixin.visitEnd();
        var field = writer.visitField(Opcodes.ACC_PRIVATE, "field_1",
                "Lnet/minecraft/Value;", null, null);
        field.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;", true).visitEnd();
        field.visitEnd();
        var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "read",
                "()Lnet/minecraft/Value;", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, "fixture/TargetMixin", "field_1",
                "Lnet/minecraft/Value;");
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
        writer.visitEnd();

        byte[] output = new MixinShadowMemberRemapper(TinyMappingIndex.read(mappingFile))
                .transform(writer.toByteArray());
        ClassNode node = new ClassNode();
        new ClassReader(output).accept(node, 0);
        assertThat(node.fields).extracting(value -> value.name).containsExactly("value");
        FieldInsnNode reference = (FieldInsnNode) node.methods.stream()
                .filter(value -> value.name.equals("read")).findFirst().orElseThrow()
                .instructions.get(1);
        assertThat(reference.name).isEqualTo("value");
    }
}
