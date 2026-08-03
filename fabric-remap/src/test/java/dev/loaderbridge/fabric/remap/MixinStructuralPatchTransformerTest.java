package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class MixinStructuralPatchTransformerTest {
    @Test
    void retargetsForgeMovedBlockNotificationCallWithoutUsingModIdentity() {
        byte[] output = new MixinStructuralPatchTransformer("1.21.1", "52.1.0")
                .transform(levelMixin("arbitrary/package/AnyMixin"));
        AtomicReference<String> target = new AtomicReference<>();
        new ClassReader(output).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        return nestedTarget(target);
                    }
                };
            }
        }, 0);

        assertThat(target.get()).contains(";markAndNotifyBlock(")
                .contains("LevelChunk;")
                .doesNotContain(";onBlockStateChange(");
    }

    @Test
    void doesNotApplyOutsidePinnedForgeLine() {
        byte[] input = levelMixin("fixture/UntouchedMixin");
        byte[] output = new MixinStructuralPatchTransformer("1.21.1", "53.0.0")
                .transform(input);
        assertThat(output).isSameAs(input);
    }

    @Test
    void movesSafeMutableShadowMapReplacementToConstructorReturn() {
        byte[] output = new MixinStructuralPatchTransformer("1.21.1", "52.1.0")
                .transform(levelChunkMapMixin());
        AtomicReference<String> point = new AtomicReference<>();
        AtomicReference<String> target = new AtomicReference<>();
        new ClassReader(output).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        return constructorPoint(point, target);
                    }
                };
            }
        }, 0);
        assertThat(point).hasValue("RETURN");
        assertThat(target).hasValue(null);
    }

    @Test
    void movesNullClearingOfMutableShadowFieldsToConstructorReturn() {
        byte[] output = new MixinStructuralPatchTransformer("1.21.1", "52.1.0")
                .transform(nullClearingMixin());
        AtomicReference<String> point = new AtomicReference<>();
        AtomicReference<String> target = new AtomicReference<>();
        new ClassReader(output).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        return constructorPoint(point, target);
                    }
                };
            }
        }, 0);
        assertThat(point).hasValue("RETURN");
        assertThat(target.get()).isNull();
    }

    @Test
    void movesArgumentIndependentStaticHookToConstructorReturn() {
        byte[] output = new MixinStructuralPatchTransformer("1.21.1", "52.1.0")
                .transform(staticHookMixin());
        AtomicReference<String> point = new AtomicReference<>();
        AtomicReference<String> target = new AtomicReference<>();
        new ClassReader(output).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        return constructorPoint(point, target);
                    }
                };
            }
        }, 0);
        assertThat(point).hasValue("RETURN");
        assertThat(target).hasValue(null);
    }

    @Test
    void makesFabricBoatMapHookOptionalWhenForgeRemovedTheCallsite() {
        byte[] output = new MixinStructuralPatchTransformer("1.21.1", "52.1.0")
                .transform(boatRendererMixin());
        AtomicReference<Integer> require = new AtomicReference<>();
        new ClassReader(output).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public AnnotationVisitor visitAnnotation(String descriptor,
                            boolean visible) {
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override public void visit(String name, Object value) {
                                if (name.equals("require")) require.set((Integer) value);
                            }
                        };
                    }
                };
            }
        }, 0);
        assertThat(require).hasValue(0);
    }

    @Test
    void addsForgeBoatModelOverrideToOldFabricRendererShape() {
        byte[] output = new MixinStructuralPatchTransformer("1.21.1", "52.1.0")
                .transform(fabricBoatRenderer());
        AtomicReference<String> override = new AtomicReference<>();
        AtomicBoolean hasFrame = new AtomicBoolean();
        new ClassReader(output).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                if (name.equals("getModelWithLocation")) override.set(name + descriptor);
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitFrame(int type, int numLocal, Object[] local,
                            int numStack, Object[] stack) {
                        if (name.equals("getModelWithLocation")) hasFrame.set(true);
                    }
                };
            }
        }, 0);
        assertThat(override).hasValue("getModelWithLocation(Lnet/minecraft/world/entity/vehicle/Boat;)"
                + "Lcom/mojang/datafixers/util/Pair;");
        assertThat(hasFrame).isTrue();
    }

    @Test
    void redirectsFabricRegistryAliasAbiToForgeBridgeByCallShape() {
        byte[] output = new MixinStructuralPatchTransformer("1.21.1", "52.1.0")
                .transform(registryAliasCaller());
        AtomicReference<String> call = new AtomicReference<>();
        new ClassReader(output).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String name,
                            String descriptor, boolean isInterface) {
                        call.set(opcode + ":" + owner + ":" + name + descriptor + ":" + isInterface);
                    }
                };
            }
        }, 0);
        assertThat(call.get()).startsWith(Opcodes.INVOKESTATIC
                        + ":dev/loaderbridge/fabric/api/registry/RegistryAliasBridge:addAlias(")
                .contains("Lnet/minecraft/core/DefaultedRegistry;")
                .endsWith(")V:false");
    }

    private static AnnotationVisitor nestedTarget(AtomicReference<String> target) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                if ("target".equals(name)) target.set(value.toString());
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                return nestedTarget(target);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                return this;
            }
        };
    }

    private static byte[] registryAliasCaller() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/AliasCaller", null,
                "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "register", "(Lnet/minecraft/core/DefaultedRegistry;"
                        + "Lnet/minecraft/resources/ResourceLocation;"
                        + "Lnet/minecraft/resources/ResourceLocation;)V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitVarInsn(Opcodes.ALOAD, 2);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                "net/minecraft/core/DefaultedRegistry", "addAlias",
                "(Lnet/minecraft/resources/ResourceLocation;"
                        + "Lnet/minecraft/resources/ResourceLocation;)V", true);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(3, 3);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static AnnotationVisitor constructorPoint(AtomicReference<String> point,
            AtomicReference<String> target) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                if ("value".equals(name) && value instanceof String text) point.set(text);
                if ("target".equals(name)) target.set(value.toString());
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                return constructorPoint(point, target);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                return this;
            }
        };
    }

    private static byte[] levelChunkMapMixin() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/MapMixin", null,
                "java/lang/Object", null);
        AnnotationVisitor mixin = writer.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/Mixin;", false);
        AnnotationVisitor targets = mixin.visitArray("value");
        targets.visit(null, Type.getObjectType("net/minecraft/world/level/chunk/LevelChunk"));
        targets.visitEnd();
        mixin.visitEnd();
        var field = writer.visitField(Opcodes.ACC_PRIVATE, "map", "Ljava/util/Map;", null, null);
        field.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;", true).visitEnd();
        field.visitAnnotation("Lorg/spongepowered/asm/mixin/Mutable;", true).visitEnd();
        field.visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE, "replace", "()V", null, null);
        AnnotationVisitor inject = method.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/injection/Inject;", true);
        AnnotationVisitor methods = inject.visitArray("method");
        methods.visit(null, "<init>(Ljava/lang/Object;)V");
        methods.visitEnd();
        AnnotationVisitor points = inject.visitArray("at");
        AnnotationVisitor at = points.visitAnnotation(null,
                "Lorg/spongepowered/asm/mixin/injection/At;");
        at.visit("value", "INVOKE_ASSIGN");
        at.visit("target", "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;");
        at.visitEnum("shift", "Lorg/spongepowered/asm/mixin/injection/At$Shift;", "AFTER");
        at.visitEnd();
        points.visitEnd();
        inject.visitEnd();
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitTypeInsn(Opcodes.NEW,
                "it/unimi/dsi/fastutil/objects/Object2ObjectOpenHashMap");
        method.visitInsn(Opcodes.DUP);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "it/unimi/dsi/fastutil/objects/Object2ObjectOpenHashMap", "<init>", "()V", false);
        method.visitFieldInsn(Opcodes.PUTFIELD, "fixture/MapMixin", "map", "Ljava/util/Map;");
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(3, 1);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] nullClearingMixin() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/ClearMixin", null,
                "java/lang/Object", null);
        AnnotationVisitor mixin = writer.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/Mixin;", false);
        AnnotationVisitor targets = mixin.visitArray("value");
        targets.visit(null, Type.getObjectType(
                "net/minecraft/world/phys/shapes/EntityCollisionContext"));
        targets.visitEnd();
        mixin.visitEnd();
        var field = writer.visitField(Opcodes.ACC_PRIVATE, "cached", "Ljava/lang/Object;", null, null);
        field.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;", true).visitEnd();
        field.visitAnnotation("Lorg/spongepowered/asm/mixin/Mutable;", true).visitEnd();
        field.visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE, "clear", "()V", null, null);
        AnnotationVisitor inject = method.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/injection/Inject;", true);
        AnnotationVisitor methods = inject.visitArray("method");
        methods.visit(null, "<init>(Ljava/lang/Object;)V");
        methods.visitEnd();
        AnnotationVisitor points = inject.visitArray("at");
        AnnotationVisitor at = points.visitAnnotation(null,
                "Lorg/spongepowered/asm/mixin/injection/At;");
        at.visit("value", "INVOKE");
        at.visit("target", "Lfixture/Target;<init>()V");
        at.visitEnum("shift", "Lorg/spongepowered/asm/mixin/injection/At$Shift;", "AFTER");
        at.visitEnd();
        points.visitEnd();
        inject.visitEnd();
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitFieldInsn(Opcodes.PUTFIELD, "fixture/ClearMixin", "cached", "Ljava/lang/Object;");
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(2, 1);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] staticHookMixin() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/StaticHookMixin", null,
                "java/lang/Object", null);
        AnnotationVisitor mixin = writer.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/Mixin;", false);
        AnnotationVisitor targets = mixin.visitArray("value");
        targets.visit(null, Type.getObjectType("net/minecraft/client/Minecraft"));
        targets.visitEnd();
        mixin.visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE, "afterModels",
                "(Ljava/lang/Object;)V", null, null);
        AnnotationVisitor inject = method.visitAnnotation(INJECT_DESCRIPTOR, true);
        AnnotationVisitor methods = inject.visitArray("method");
        methods.visit(null, "<init>(Ljava/lang/Object;)V");
        methods.visitEnd();
        AnnotationVisitor points = inject.visitArray("at");
        AnnotationVisitor at = points.visitAnnotation(null, AT_DESCRIPTOR);
        at.visit("value", "INVOKE");
        at.visit("target", "Lfixture/Renderer;<init>()V");
        at.visitEnd();
        points.visitEnd();
        inject.visitEnd();
        method.visitCode();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "fixture/Hooks", "register", "()V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 2);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static final String INJECT_DESCRIPTOR =
            "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String AT_DESCRIPTOR =
            "Lorg/spongepowered/asm/mixin/injection/At;";

    private static byte[] boatRendererMixin() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/BoatRendererMixin", null,
                "java/lang/Object", null);
        AnnotationVisitor mixin = writer.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/Mixin;", false);
        AnnotationVisitor targets = mixin.visitArray("value");
        targets.visit(null, Type.getObjectType("net/minecraft/client/renderer/entity/BoatRenderer"));
        targets.visitEnd();
        mixin.visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE, "wrap", "()V", null, null);
        AnnotationVisitor wrap = method.visitAnnotation(
                "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;", true);
        AnnotationVisitor methods = wrap.visitArray("method");
        methods.visit(null, "render");
        methods.visitEnd();
        AnnotationVisitor points = wrap.visitArray("at");
        AnnotationVisitor at = points.visitAnnotation(null, AT_DESCRIPTOR);
        at.visit("value", "INVOKE");
        at.visit("target", "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;");
        at.visitEnd();
        points.visitEnd();
        wrap.visitEnd();
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] fabricBoatRenderer() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/FabricBoatRenderer", null,
                "net/minecraft/client/renderer/entity/BoatRenderer", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "getTextureAndModel",
                "(Lfixture/BoatHolder;)Lcom/mojang/datafixers/util/Pair;", null, null);
        method.visitCode();
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 2);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] levelMixin(String name) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        AnnotationVisitor mixin = writer.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/Mixin;", false);
        AnnotationVisitor targets = mixin.visitArray("value");
        targets.visit(null, Type.getObjectType("net/minecraft/world/level/Level"));
        targets.visitEnd();
        mixin.visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE, "handler", "()V", null, null);
        AnnotationVisitor inject = method.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/injection/Inject;", true);
        AnnotationVisitor methods = inject.visitArray("method");
        methods.visit(null, "setBlock(Lnet/minecraft/core/BlockPos;"
                + "Lnet/minecraft/world/level/block/state/BlockState;II)Z");
        methods.visitEnd();
        AnnotationVisitor at = inject.visitArray("at");
        AnnotationVisitor nested = at.visitAnnotation(null,
                "Lorg/spongepowered/asm/mixin/injection/At;");
        nested.visit("value", "INVOKE");
        nested.visit("target", "Lnet/minecraft/world/level/Level;onBlockStateChange("
                + "Lnet/minecraft/core/BlockPos;"
                + "Lnet/minecraft/world/level/block/state/BlockState;"
                + "Lnet/minecraft/world/level/block/state/BlockState;)V");
        nested.visitEnd();
        at.visitEnd();
        inject.visitEnd();
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
