package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Guards the complete public binary surface used by Fabric Loader 0.16.14 mods. */
class FabricLoaderBinaryContractTest {
    private static final int CLASS_ACCESS = Opcodes.ACC_PUBLIC
            | Opcodes.ACC_PROTECTED
            | Opcodes.ACC_FINAL
            | Opcodes.ACC_INTERFACE
            | Opcodes.ACC_ABSTRACT
            | Opcodes.ACC_ANNOTATION
            | Opcodes.ACC_ENUM
            | Opcodes.ACC_RECORD;
    private static final int MEMBER_ACCESS = Opcodes.ACC_PUBLIC
            | Opcodes.ACC_PROTECTED
            | Opcodes.ACC_STATIC;

    @Test
    void preservesEveryPinnedPublicTypeAndMember() throws Exception {
        Path reference = Path.of(System.getProperty("loaderbridge.fabricLoaderReferenceJar"));
        Path shim = Path.of(System.getProperty("loaderbridge.shimJar"));

        Map<String, ClassContract> expected = readContracts(reference);
        Map<String, ClassContract> actual = readContracts(shim);

        assertThat(expected).isNotEmpty();
        for (Map.Entry<String, ClassContract> entry : expected.entrySet()) {
            assertThat(actual)
                    .as("LoaderBridge classes must contain %s", entry.getKey())
                    .containsKey(entry.getKey());
            ClassContract actualClass = actual.get(entry.getKey());
            ClassContract expectedClass = entry.getValue();
            assertThat(actualClass.header())
                    .as("binary class header for %s", entry.getKey())
                    .isEqualTo(expectedClass.header());
            assertThat(actualClass.members())
                    .as("public/protected binary members for %s", entry.getKey())
                    .containsAll(expectedClass.members());
        }
    }

    private static Map<String, ClassContract> readContracts(Path jarPath) throws IOException {
        Map<String, ClassContract> contracts = new LinkedHashMap<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entries = jar.stream()
                    .filter(entry -> isContractClass(entry.getName()))
                    .sorted(java.util.Comparator.comparing(java.util.jar.JarEntry::getName))
                    .toList();
            for (var entry : entries) {
                try (InputStream input = jar.getInputStream(entry)) {
                    ClassContract contract = readContract(input);
                    if ((contract.header().access() & Opcodes.ACC_PUBLIC) != 0) {
                        contracts.put(contract.header().name(), contract);
                    }
                }
            }
        }
        return Map.copyOf(contracts);
    }

    private static boolean isContractClass(String name) {
        return name.endsWith(".class") && (name.startsWith("net/fabricmc/api/")
                || name.startsWith("net/fabricmc/loader/api/")
                || name.equals("net/fabricmc/loader/util/version/VersionParsingException.class"));
    }

    private static ClassContract readContract(InputStream input) throws IOException {
        List<MemberContract> members = new ArrayList<>();
        ClassHeader[] header = new ClassHeader[1];
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                    String superName, String[] interfaces) {
                List<String> sortedInterfaces = Arrays.stream(interfaces).sorted().toList();
                int contractAccess = access & CLASS_ACCESS;
                if ((contractAccess & Opcodes.ACC_ENUM) != 0) {
                    // Constant-specific implementations make javac mark the enum abstract rather
                    // than final. Neither flag changes an enum consumer's linkage contract.
                    contractAccess &= ~(Opcodes.ACC_ABSTRACT | Opcodes.ACC_FINAL);
                }
                header[0] = new ClassHeader(name, contractAccess, superName, sortedInterfaces);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                    String signature, Object value) {
                if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
                    members.add(new MemberContract("F", name, descriptor, access & MEMBER_ACCESS));
                }
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
                    members.add(new MemberContract("M", name, descriptor, access & MEMBER_ACCESS));
                }
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return new ClassContract(header[0], List.copyOf(members));
    }

    private record ClassHeader(String name, int access, String superName, List<String> interfaces) {}

    private record MemberContract(String kind, String name, String descriptor, int access) {}

    private record ClassContract(ClassHeader header, List<MemberContract> members) {}
}
