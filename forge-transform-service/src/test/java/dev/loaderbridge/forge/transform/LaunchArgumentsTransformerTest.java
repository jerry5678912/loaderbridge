package dev.loaderbridge.forge.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

class LaunchArgumentsTransformerTest {
    @Test
    void capturesArgumentsAtTheStartOfMinecraftMainExactlyOnce() {
        ClassNode input = minecraftMain();
        var transformer = new LaunchArgumentsTransformer();

        transformer.transform(input, null);
        transformer.transform(input, null);

        MethodNode main = input.methods.getFirst();
        assertThat(main.instructions.toArray()).hasSize(3);
        assertThat(main.instructions.getFirst()).isInstanceOfSatisfying(
                VarInsnNode.class, load -> {
                    assertThat(load.getOpcode()).isEqualTo(Opcodes.ALOAD);
                    assertThat(load.var).isZero();
                });
        assertThat(main.instructions.getFirst().getNext()).isInstanceOfSatisfying(
                MethodInsnNode.class, call -> {
                    assertThat(call.getOpcode()).isEqualTo(Opcodes.INVOKESTATIC);
                    assertThat(call.owner).isEqualTo(
                            "dev/loaderbridge/fabric/runtime/BridgeFabricLoader");
                    assertThat(call.name).isEqualTo("captureLaunchArguments");
                    assertThat(call.desc).isEqualTo("([Ljava/lang/String;)V");
                });
        assertThat(main.maxStack).isEqualTo(1);
    }

    @Test
    void rejectsAnIncompatibleMinecraftMainShapeWithAStableDiagnostic() {
        ClassNode input = new ClassNode(Opcodes.ASM9);
        input.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "net/minecraft/server/Main", null,
                "java/lang/Object", null);

        assertThatThrownBy(() -> new LaunchArgumentsTransformer().transform(input, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LB-LOADER-ARG-001", "net/minecraft/server/Main");
    }

    private static ClassNode minecraftMain() {
        ClassNode input = new ClassNode(Opcodes.ASM9);
        input.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "net/minecraft/server/Main", null,
                "java/lang/Object", null);
        MethodNode main = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "main", "([Ljava/lang/String;)V", null, null);
        main.instructions.add(new InsnNode(Opcodes.RETURN));
        input.methods.add(main);
        return input;
    }
}
