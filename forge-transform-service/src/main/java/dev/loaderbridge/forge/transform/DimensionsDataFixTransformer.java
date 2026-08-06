package dev.loaderbridge.forge.transform;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Marks the two 1.21.1 world-generator choices for agent-patched fail-soft DFU. */
final class DimensionsDataFixTransformer implements ITransformer<ClassNode> {
    static final String TAGGED_CHOICE =
            "com/mojang/datafixers/types/templates/TaggedChoice";
    static final String V2832 = "net/minecraft/util/datafix/schemas/V2832";
    private static final String SETTER = "loaderbridge$setDimensionsFailSoft";
    private static final String SETTER_DESCRIPTOR = "(Z)V";
    private static final Set<Target> TARGETS = Set.of(
            Target.targetPreClass(V2832.replace('/', '.')));

    @Override public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        if (!input.name.equals(V2832)) {
            throw new IllegalStateException(
                    "LB-DIM-001: unexpected dimensions transform target " + input.name);
        }
        int patched = 0;
        for (String name : Set.of("lambda$registerTypes$7", "lambda$registerTypes$6")) {
            MethodNode method = requireMethod(input, name,
                    "(Lcom/mojang/datafixers/schemas/Schema;)Lcom/mojang/datafixers/types/templates/TypeTemplate;");
            for (var instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)
                        || !call.owner.equals("com/mojang/datafixers/DSL")
                        || !call.name.equals("taggedChoiceLazy")) continue;
                InsnList patch = new InsnList();
                patch.add(new InsnNode(Opcodes.DUP));
                patch.add(new InsnNode(Opcodes.ICONST_1));
                patch.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TAGGED_CHOICE,
                        SETTER, SETTER_DESCRIPTOR, false));
                method.instructions.insert(call, patch);
                method.maxStack = Math.max(method.maxStack, 4);
                patched++;
            }
        }
        if (patched != 2) {
            throw new IllegalStateException("LB-DIM-002: incompatible " + input.name
                    + " shape; expected two taggedChoiceLazy factories");
        }
        return input;
    }

    private static MethodNode requireMethod(ClassNode input, String name, String descriptor) {
        return input.methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "LB-DIM-002: incompatible " + input.name + " shape; missing "
                                + name + descriptor));
    }

    @Override public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override public Set<Target> targets() { return TARGETS; }
}
