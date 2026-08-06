package dev.loaderbridge.agent.dimensions;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

final class DimensionsDfuTransformer implements ClassFileTransformer {
    static final String TAGGED_CHOICE =
            "com/mojang/datafixers/types/templates/TaggedChoice";
    static final String TAGGED_CHOICE_TYPE = TAGGED_CHOICE + "$TaggedChoiceType";
    private static final String FLAG = "loaderbridge$dimensionsFailSoft";
    private static final String SETTER = "loaderbridge$setDimensionsFailSoft";
    private static final String SETTER_DESCRIPTOR = "(Z)V";

    @Override public byte[] transform(Module module, ClassLoader loader, String className,
            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {
        if (!TAGGED_CHOICE.equals(className) && !TAGGED_CHOICE_TYPE.equals(className)) {
            return null;
        }
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(classfileBuffer).accept(node, 0);
        if (TAGGED_CHOICE.equals(className)) transformTaggedChoice(node);
        else transformTaggedChoiceType(node);
        ClassWriter writer = new ConservativeClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void transformTaggedChoice(ClassNode input) {
        addFlagAndSetter(input);
        MethodNode method = requireMethod(input, "lambda$apply$0",
                "(Lcom/mojang/datafixers/util/Pair;)Lcom/mojang/datafixers/types/Type;");
        boolean patched = false;
        for (var instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.ARETURN) continue;
            LabelNode skip = new LabelNode();
            InsnList patch = new InsnList();
            patch.add(new InsnNode(Opcodes.DUP));
            patch.add(new TypeInsnNode(Opcodes.INSTANCEOF, TAGGED_CHOICE_TYPE));
            patch.add(new JumpInsnNode(Opcodes.IFEQ, skip));
            patch.add(new InsnNode(Opcodes.DUP));
            patch.add(new TypeInsnNode(Opcodes.CHECKCAST, TAGGED_CHOICE_TYPE));
            patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
            patch.add(new FieldInsnNode(Opcodes.GETFIELD, TAGGED_CHOICE, FLAG, "Z"));
            patch.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TAGGED_CHOICE_TYPE,
                    SETTER, SETTER_DESCRIPTOR, false));
            patch.add(skip);
            method.instructions.insertBefore(instruction, patch);
            patched = true;
        }
        if (!patched) throw incompatible(input, "lambda$apply$0 return");
    }

    private static void transformTaggedChoiceType(ClassNode input) {
        addFlagAndSetter(input);
        MethodNode method = requireMethod(input, "getMapCodec",
                "(Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;");
        LabelNode nativePath = new LabelNode();
        InsnList patch = new InsnList();
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(new FieldInsnNode(Opcodes.GETFIELD, TAGGED_CHOICE_TYPE, FLAG, "Z"));
        patch.add(new JumpInsnNode(Opcodes.IFEQ, nativePath));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(new FieldInsnNode(Opcodes.GETFIELD, TAGGED_CHOICE_TYPE, "types",
                "Lit/unimi/dsi/fastutil/objects/Object2ObjectMap;"));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 1));
        patch.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "it/unimi/dsi/fastutil/objects/Object2ObjectMap", "containsKey",
                "(Ljava/lang/Object;)Z", true));
        patch.add(new JumpInsnNode(Opcodes.IFNE, nativePath));
        patch.add(new FieldInsnNode(Opcodes.GETSTATIC, "com/mojang/serialization/Codec",
                "PASSTHROUGH", "Lcom/mojang/serialization/Codec;"));
        patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/mojang/serialization/MapCodec", "assumeMapUnsafe",
                "(Lcom/mojang/serialization/Codec;)Lcom/mojang/serialization/MapCodec;", false));
        patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/mojang/serialization/DataResult", "success",
                "(Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", true));
        patch.add(new InsnNode(Opcodes.ARETURN));
        patch.add(nativePath);
        method.instructions.insert(patch);
    }

    private static void addFlagAndSetter(ClassNode input) {
        if (input.fields.stream().anyMatch(field -> field.name.equals(FLAG))
                || input.methods.stream().anyMatch(method -> method.name.equals(SETTER))) {
            throw incompatible(input, "reserved bridge members are already present");
        }
        input.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, FLAG, "Z", null, null));
        MethodNode setter = new MethodNode(Opcodes.ACC_PUBLIC, SETTER, SETTER_DESCRIPTOR,
                null, null);
        setter.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        setter.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        setter.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, input.name, FLAG, "Z"));
        setter.instructions.add(new InsnNode(Opcodes.RETURN));
        setter.maxStack = 2;
        setter.maxLocals = 2;
        input.methods.add(setter);
    }

    private static MethodNode requireMethod(ClassNode input, String name, String descriptor) {
        return input.methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .findFirst().orElseThrow(() -> incompatible(input, name + descriptor));
    }

    private static IllegalStateException incompatible(ClassNode input, String shape) {
        return new IllegalStateException("LB-DIM-AGENT-003: incompatible " + input.name
                + " shape; missing " + shape);
    }

    private static final class ConservativeClassWriter extends ClassWriter {
        ConservativeClassWriter(int flags) { super(flags); }

        @Override protected String getCommonSuperClass(String first, String second) {
            return "java/lang/Object";
        }
    }
}
