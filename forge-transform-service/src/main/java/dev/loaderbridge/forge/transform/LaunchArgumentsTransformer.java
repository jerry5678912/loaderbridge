package dev.loaderbridge.forge.transform;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Publishes the final game argument array before either Minecraft target parses it. */
final class LaunchArgumentsTransformer implements ITransformer<ClassNode> {
    private static final String CAPTURE_OWNER =
            "dev/loaderbridge/fabric/runtime/BridgeFabricLoader";
    private static final String MAIN_DESCRIPTOR = "([Ljava/lang/String;)V";
    private static final Set<Target> TARGETS = Set.of(
            Target.targetClass("net.minecraft.client.main.Main"),
            Target.targetClass("net.minecraft.server.Main"));

    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        var main = input.methods.stream()
                .filter(method -> method.name.equals("main"))
                .filter(method -> method.desc.equals(MAIN_DESCRIPTOR))
                .filter(method -> (method.access & Opcodes.ACC_STATIC) != 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "LB-LOADER-ARG-001: Minecraft main(String[]) is unavailable in "
                                + input.name));
        boolean installed = main.instructions.getFirst() instanceof VarInsnNode load
                && load.getOpcode() == Opcodes.ALOAD
                && load.var == 0
                && load.getNext() instanceof MethodInsnNode call
                && call.getOpcode() == Opcodes.INVOKESTATIC
                && call.owner.equals(CAPTURE_OWNER)
                && call.name.equals("captureLaunchArguments")
                && call.desc.equals(MAIN_DESCRIPTOR);
        if (!installed) {
            InsnList capture = new InsnList();
            capture.add(new VarInsnNode(Opcodes.ALOAD, 0));
            capture.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CAPTURE_OWNER,
                    "captureLaunchArguments", MAIN_DESCRIPTOR, false));
            main.instructions.insert(capture);
            main.maxStack = Math.max(main.maxStack, 1);
        }
        return input;
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public Set<Target> targets() {
        return TARGETS;
    }
}
