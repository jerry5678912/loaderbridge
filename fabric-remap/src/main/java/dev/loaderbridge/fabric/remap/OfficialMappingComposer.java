package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

/** Composes Fabric's intermediary mapping with Mojang's official-name ProGuard mapping. */
public final class OfficialMappingComposer {
    public void compose(Path intermediaryMappings, Path mojangMappings, Path output) throws IOException {
        MemoryMappingTree intermediary = new MemoryMappingTree();
        MappingReader.read(intermediaryMappings, MappingFormat.TINY_2_FILE, intermediary);
        MemoryMappingTree mojang = new MemoryMappingTree();
        MappingReader.read(mojangMappings, MappingFormat.PROGUARD_FILE, mojang);
        int intermediaryNamespace = requiredNamespace(intermediary, "intermediary");
        int obfuscatedNamespace = requiredNamespace(mojang, "target");

        StringBuilder tiny = new StringBuilder("tiny\t2\t0\tintermediary\tnamed\n");
        intermediary.getClasses().stream()
                .filter(mapping -> mapping.getName(intermediaryNamespace) != null)
                .sorted(Comparator.comparing(mapping -> mapping.getName(intermediaryNamespace)))
                .forEach(mapping -> appendClass(tiny, intermediary, mojang, mapping,
                        intermediaryNamespace, obfuscatedNamespace));
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, tiny, StandardCharsets.UTF_8);
    }

    private static void appendClass(StringBuilder output, MemoryMappingTree intermediary,
            MemoryMappingTree mojang, MappingTreeView.ClassMappingView mapping,
            int intermediaryNamespace, int obfuscatedNamespace) {
        String obfuscatedName = mapping.getSrcName();
        String intermediaryName = mapping.getName(intermediaryNamespace);
        MappingTreeView.ClassMappingView officialClass = mojang.getClass(obfuscatedName, obfuscatedNamespace);
        String namedName = officialClass == null ? obfuscatedName : officialClass.getSrcName();
        output.append("c\t").append(intermediaryName).append('\t').append(namedName).append('\n');

        mapping.getFields().stream()
                .filter(field -> field.getName(intermediaryNamespace) != null)
                .sorted(Comparator.comparing(field -> field.getName(intermediaryNamespace)))
                .forEach(field -> appendMember(output, intermediary, officialClass, field,
                        intermediaryNamespace, obfuscatedNamespace, false));
        mapping.getMethods().stream()
                .filter(method -> method.getName(intermediaryNamespace) != null)
                .sorted(Comparator.comparing(method -> method.getName(intermediaryNamespace)
                        + method.getDesc(intermediaryNamespace)))
                .forEach(method -> appendMember(output, intermediary, officialClass, method,
                        intermediaryNamespace, obfuscatedNamespace, true));
    }

    private static void appendMember(StringBuilder output, MemoryMappingTree intermediary,
            MappingTreeView.ClassMappingView officialClass, MappingTreeView.MemberMappingView member,
            int intermediaryNamespace, int obfuscatedNamespace, boolean method) {
        String sourceDescriptor = intermediary.mapDesc(member.getSrcDesc(), intermediaryNamespace);
        MappingTreeView.MemberMappingView officialMember = null;
        if (officialClass != null) {
            officialMember = method
                    ? officialClass.getMethod(member.getSrcName(), member.getSrcDesc(), obfuscatedNamespace)
                    : officialClass.getField(member.getSrcName(), member.getSrcDesc(), obfuscatedNamespace);
        }
        String namedName = officialMember == null ? member.getSrcName() : officialMember.getSrcName();
        output.append('\t').append(method ? 'm' : 'f').append('\t')
                .append(sourceDescriptor).append('\t')
                .append(member.getName(intermediaryNamespace)).append('\t')
                .append(namedName).append('\n');
    }

    private static int requiredNamespace(MemoryMappingTree tree, String namespace) throws IOException {
        int id = tree.getNamespaceId(namespace);
        if (id == MappingTreeView.NULL_NAMESPACE_ID) {
            throw new ArtifactVerificationException("Mapping is missing namespace: " + namespace);
        }
        return id;
    }
}
