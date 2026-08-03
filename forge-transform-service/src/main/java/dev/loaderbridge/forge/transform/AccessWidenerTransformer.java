package dev.loaderbridge.forge.transform;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/** Applies the merged, runtime-namespaced Fabric access-widener rules. */
final class AccessWidenerTransformer implements ITransformer<ClassNode> {
    private final AccessWidener accessWidener;
    private final Set<Target> targets;

    AccessWidenerTransformer(AccessWidener accessWidener) {
        this.accessWidener = java.util.Objects.requireNonNull(accessWidener, "accessWidener");
        this.targets = accessWidener.getTargets().stream()
                .map(Target::targetPreClass)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        ClassNode output = new ClassNode(Opcodes.ASM9);
        input.accept(AccessWidenerClassVisitor.createClassVisitor(
                Opcodes.ASM9, output, accessWidener));
        return output;
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public Set<Target> targets() {
        return targets;
    }
}
