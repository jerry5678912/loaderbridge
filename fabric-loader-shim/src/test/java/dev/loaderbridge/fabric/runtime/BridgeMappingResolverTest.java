package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLClassLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BridgeMappingResolverTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void mapsAndUnmapsTinyV2ClassesFieldsAndMethods() throws Exception {
        Path mappings = temporaryDirectory.resolve("mappings.tiny");
        Files.writeString(mappings, """
                tiny\t2\t0\tintermediary\tnamed
                c\tnet/minecraft/class_310\tnet/minecraft/client/Minecraft
                \tf\tLnet/minecraft/class_315;\tfield_1690\toptions
                \tm\t()V\tmethod_1507\tstop
                """);
        BridgeMappingResolver resolver = new BridgeMappingResolver();
        resolver.install(mappings);

        assertThat(resolver.getNamespaces()).containsExactly("intermediary", "named", "official");
        assertThat(resolver.getCurrentRuntimeNamespace()).isEqualTo("named");
        assertThat(resolver.mapClassName("intermediary", "net.minecraft.class_310"))
                .isEqualTo("net.minecraft.client.Minecraft");
        assertThat(resolver.unmapClassName("intermediary", "net.minecraft.client.Minecraft"))
                .isEqualTo("net.minecraft.class_310");
        assertThat(resolver.mapFieldName("intermediary", "net.minecraft.class_310",
                "field_1690", "Lnet/minecraft/class_315;"))
                .isEqualTo("options");
        assertThat(resolver.mapMethodName("intermediary", "net.minecraft.class_310",
                "method_1507", "()V")).isEqualTo("stop");
        assertThat(resolver.mapClassName("named", "example.Unmapped"))
                .isEqualTo("example.Unmapped");
    }

    @Test
    void rejectsInvalidNamespaceClassFormatAndMappingHeader() throws Exception {
        BridgeMappingResolver resolver = new BridgeMappingResolver();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.mapClassName("unknown", "example.Class"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.mapClassName("named", "example/Class"));

        Path mappings = temporaryDirectory.resolve("invalid.tiny");
        Files.writeString(mappings, "tiny\t2\t0\tnamed\tintermediary\n");
        assertThatThrownBy(() -> resolver.install(mappings))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("LB-MAP-001");
    }

    @Test
    void discoversMappingsFromTranslatedModResourcesBeforeContainersAreConstructed() throws Exception {
        Path resource = temporaryDirectory.resolve("META-INF/loaderbridge/mappings.tiny");
        Files.createDirectories(resource.getParent());
        Files.writeString(resource, """
                tiny\t2\t0\tintermediary\tnamed
                c\tnet/minecraft/class_2688\tnet/minecraft/world/level/block/state/StateHolder
                \tf\tLjava/util/Map;\tfield_24739\tvalues
                """);
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader translatedModLayer = new URLClassLoader(
                new java.net.URL[] { temporaryDirectory.toUri().toURL() }, null)) {
            Thread.currentThread().setContextClassLoader(translatedModLayer);
            BridgeMappingResolver resolver = new BridgeMappingResolver();

            assertThat(resolver.mapFieldName("intermediary", "net.minecraft.class_2688",
                    "field_24739", "Ljava/util/Map;")).isEqualTo("values");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
