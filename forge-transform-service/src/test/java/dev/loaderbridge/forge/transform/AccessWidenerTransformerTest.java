package dev.loaderbridge.forge.transform;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

class AccessWidenerTransformerTest {
    @Test
    void appliesClassFieldMethodFinalityInheritanceAndTransitiveRulesBeforeDefinition() {
        AccessWidener widener = new AccessWidener();
        new AccessWidenerReader(widener).read("""
                accessWidener v2 official
                accessible class net/minecraft/Example
                extendable class net/minecraft/Example
                accessible field net/minecraft/Example exposed I
                mutable field net/minecraft/Example mutable I
                accessible method net/minecraft/Example exposedMethod ()V
                extendable method net/minecraft/Example overridable ()V
                accessible method net/minecraft/Example <init> ()V
                transitive-accessible field net/minecraft/Example transitive I
                """.getBytes(StandardCharsets.UTF_8), "official");
        ClassNode input = new ClassNode(Opcodes.ASM9);
        input.visit(Opcodes.V21, Opcodes.ACC_FINAL, "net/minecraft/Example", null,
                "java/lang/Object", null);
        input.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "exposed", "I", null, null));
        input.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "mutable", "I", null, null));
        input.fields.add(new FieldNode(Opcodes.ACC_PRIVATE,
                "transitive", "I", null, null));
        input.methods.add(new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "exposedMethod", "()V", null, null));
        input.methods.add(new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "overridable", "()V", null, null));
        input.methods.add(new MethodNode(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null));

        ClassNode output = new AccessWidenerTransformer(widener).transform(input, null);

        assertThat(output.access & Opcodes.ACC_PUBLIC).isNotZero();
        assertThat(output.access & Opcodes.ACC_FINAL).isZero();
        assertThat(output.fields.get(0).access & Opcodes.ACC_PUBLIC).isNotZero();
        assertThat(output.fields.get(1).access & Opcodes.ACC_FINAL).isZero();
        assertThat(output.fields.get(2).access & Opcodes.ACC_PUBLIC).isNotZero();
        assertThat(output.methods.get(0).access & Opcodes.ACC_PUBLIC).isNotZero();
        assertThat(output.methods.get(1).access & Opcodes.ACC_FINAL).isZero();
        assertThat(output.methods.get(1).access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED))
                .isNotZero();
        assertThat(output.methods.get(2).access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED))
                .isNotZero();
        assertThat(new AccessWidenerTransformer(widener).targets())
                .extracting(target -> target.getClassName())
                .contains("net.minecraft.Example");
    }
}
