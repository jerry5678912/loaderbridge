package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AccessWidenerResourceTransformerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void remapsV2ClassesMembersDescriptorsAndTransitiveRules() throws Exception {
        Path mappings = temporaryDirectory.resolve("mappings.tiny");
        Files.writeString(mappings, "tiny\t2\t0\tintermediary\tnamed\n"
                + "c\tnet/minecraft/class_1\tnet/minecraft/Example\n"
                + "\tf\tLnet/minecraft/class_1;\tfield_1\tvalue\n"
                + "\tm\t(Lnet/minecraft/class_1;)V\tmethod_1\trun\n");
        byte[] input = """
                accessWidener v2 intermediary
                accessible class net/minecraft/class_1
                transitive-mutable field net/minecraft/class_1 field_1 Lnet/minecraft/class_1;
                extendable method net/minecraft/class_1 method_1 (Lnet/minecraft/class_1;)V
                """.getBytes(StandardCharsets.UTF_8);

        byte[] output = new AccessWidenerResourceTransformer().transform(
                input, TinyMappingIndex.read(mappings));

        assertThat(new String(output, StandardCharsets.UTF_8)).contains(
                "accessWidener\tv2\tofficial",
                "accessible\tclass\tnet/minecraft/Example",
                "transitive-mutable\tfield\tnet/minecraft/Example\tvalue\tLnet/minecraft/Example;",
                "extendable\tmethod\tnet/minecraft/Example\trun\t(Lnet/minecraft/Example;)V");
    }

    @Test
    void rejectsIntermediaryRulesWithoutMappings() {
        byte[] input = "accessWidener v1 intermediary\naccessible class net/minecraft/class_1\n"
                .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> new AccessWidenerResourceTransformer().transform(input, null))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("LB-AW-002");
    }
}
