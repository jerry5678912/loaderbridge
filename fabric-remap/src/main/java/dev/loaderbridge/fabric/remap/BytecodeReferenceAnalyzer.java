package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Static class-file inventory. This analyzer never defines or initializes inspected classes. */
public final class BytecodeReferenceAnalyzer {
    public ReferenceInventory analyze(Path artifact) throws IOException {
        Set<String> fabricApi = new LinkedHashSet<>();
        Set<String> loaderApi = new LinkedHashSet<>();
        Set<String> minecraft = new LinkedHashSet<>();
        Set<String> strings = new LinkedHashSet<>();
        Set<String> natives = new LinkedHashSet<>();
        try (JarFile jar = new JarFile(artifact.toFile(), false)) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                if (isNative(entry.getName())) {
                    natives.add(entry.getName());
                } else if (entry.getName().endsWith(".class")) {
                    try (InputStream input = jar.getInputStream(entry)) {
                        new ClassReader(input).accept(new InventoryVisitor(fabricApi, loaderApi, minecraft, strings),
                                ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    }
                }
            }
        }
        return new ReferenceInventory(fabricApi, loaderApi, minecraft, strings, natives);
    }

    private static boolean isNative(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".so") || lower.endsWith(".dll") || lower.endsWith(".dylib")
                || lower.endsWith(".jnilib");
    }

    private static final class InventoryVisitor extends ClassVisitor {
        private final Set<String> fabricApi;
        private final Set<String> loaderApi;
        private final Set<String> minecraft;
        private final Set<String> strings;

        InventoryVisitor(Set<String> fabricApi, Set<String> loaderApi, Set<String> minecraft, Set<String> strings) {
            super(Opcodes.ASM9);
            this.fabricApi = fabricApi;
            this.loaderApi = loaderApi;
            this.minecraft = minecraft;
            this.strings = strings;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            collectDescriptor(descriptor);
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitTypeInsn(int opcode, String type) {
                    collect(type);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                    collect(owner);
                    collectDescriptor(fieldDescriptor);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String methodName,
                        String methodDescriptor, boolean isInterface) {
                    collect(owner);
                    collectDescriptor(methodDescriptor);
                }

                @Override
                public void visitInvokeDynamicInsn(String invokedName, String invokedDescriptor,
                        Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                    collectDescriptor(invokedDescriptor);
                    collect(bootstrapMethodHandle.getOwner());
                    for (Object argument : bootstrapMethodArguments) {
                        if (argument instanceof Type type) {
                            collectDescriptor(type.getDescriptor());
                        } else if (argument instanceof Handle handle) {
                            collect(handle.getOwner());
                        }
                    }
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof String text && (text.startsWith("net.fabricmc.")
                            || text.startsWith("net/minecraft/"))) {
                        strings.add(text);
                    } else if (value instanceof Type type) {
                        collectDescriptor(type.getDescriptor());
                    }
                }
            };
        }

        private void collectDescriptor(String descriptor) {
            Type type = Type.getType(descriptor);
            if (type.getSort() == Type.METHOD) {
                collectType(type.getReturnType());
                for (Type argument : type.getArgumentTypes()) {
                    collectType(argument);
                }
            } else {
                collectType(type);
            }
        }

        private void collectType(Type type) {
            Type current = type;
            while (current.getSort() == Type.ARRAY) {
                current = current.getElementType();
            }
            if (current.getSort() == Type.OBJECT) {
                collect(current.getInternalName());
            }
        }

        private void collect(String internalName) {
            String binaryName = internalName.replace('/', '.');
            if (binaryName.startsWith("net.fabricmc.fabric.api.")) {
                fabricApi.add(binaryName);
            } else if (binaryName.startsWith("net.fabricmc.loader.api.")) {
                loaderApi.add(binaryName);
            } else if (binaryName.startsWith("net.minecraft.")) {
                minecraft.add(binaryName);
            }
        }
    }
}
