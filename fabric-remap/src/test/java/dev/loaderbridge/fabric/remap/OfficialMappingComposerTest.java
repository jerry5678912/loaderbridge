package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OfficialMappingComposerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void composesFabricIntermediaryNamesWithMojangOfficialNames() throws Exception {
        Path intermediary = temporaryDirectory.resolve("intermediary.tiny");
        Files.writeString(intermediary, "tiny\t2\t0\tofficial\tintermediary\n"
                + "c\ta\tnet/minecraft/class_1\n"
                + "\tm\t()V\tb\tmethod_1\n"
                + "\tf\tI\tc\tfield_1\n");
        Path mojang = temporaryDirectory.resolve("client.txt");
        Files.writeString(mojang, "net.minecraft.Example -> a:\n"
                + "    void run() -> b\n"
                + "    int count -> c\n");
        Path output = temporaryDirectory.resolve("intermediary-to-named.tiny");

        new OfficialMappingComposer().compose(intermediary, mojang, output);

        MemoryMappingTree tree = new MemoryMappingTree();
        MappingReader.read(output, tree);
        int named = tree.getNamespaceId("named");
        var classMapping = tree.getClass("net/minecraft/class_1");
        assertThat(tree.getSrcNamespace()).isEqualTo("intermediary");
        assertThat(classMapping.getName(named)).isEqualTo("net/minecraft/Example");
        assertThat(classMapping.getMethod("method_1", "()V").getName(named)).isEqualTo("run");
        assertThat(classMapping.getField("field_1", "I").getName(named)).isEqualTo("count");
    }
}
