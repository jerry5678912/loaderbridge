package dev.loaderbridge.forge.transform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class DimensionsDataFixTransformerTest {
    @Test
    void marksBothWorldGeneratorTaggedChoicesForAgentPatchedFailSoftDfu() {
        ClassNode input = new ClassNode(Opcodes.ASM9);
        input.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, DimensionsDataFixTransformer.V2832,
                null, "java/lang/Object", null);
        input.methods.add(v2832Factory("lambda$registerTypes$7"));
        input.methods.add(v2832Factory("lambda$registerTypes$6"));

        var transformer = new DimensionsDataFixTransformer();
        transformer.transform(input, null);

        assertThat(input.methods).allSatisfy(method -> assertThat(Arrays.stream(
                method.instructions.toArray())
                .filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast)
                .toList()).anySatisfy(call -> {
                    assertThat(call.owner).isEqualTo(DimensionsDataFixTransformer.TAGGED_CHOICE);
                    assertThat(call.name).isEqualTo("loaderbridge$setDimensionsFailSoft");
                }));
        assertThat(transformer.targets()).extracting(target -> target.getClassName())
                .containsExactly("net.minecraft.util.datafix.schemas.V2832");
    }

    private static MethodNode v2832Factory(String name) {
        MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, name,
                "(Lcom/mojang/datafixers/schemas/Schema;)Lcom/mojang/datafixers/types/templates/TypeTemplate;",
                null, null);
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/mojang/datafixers/DSL", "taggedChoiceLazy",
                "(Ljava/lang/String;Lcom/mojang/datafixers/types/Type;Ljava/util/Map;)Lcom/mojang/datafixers/types/templates/TaggedChoice;",
                true));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        method.maxStack = 3;
        return method;
    }
}
