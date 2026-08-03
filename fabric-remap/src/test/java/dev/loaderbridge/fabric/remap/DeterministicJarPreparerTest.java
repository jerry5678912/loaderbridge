package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.fabric.metadata.FabricModInspector;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class DeterministicJarPreparerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void producesStableUnsignedJarWithForgeAndBridgeMetadata() throws Exception {
        Path source = temporaryDirectory.resolve("source.jar");
        writeJar(source, Map.of(
                "fabric.mod.json", "{\"schemaVersion\":1,\"id\":\"fixture\",\"version\":\"1.0.0\"}",
                "example/data.txt", "preserved",
                "META-INF/OLD.SF", "invalid after transform",
                "META-INF/OLD.RSA", "invalid after transform"));

        var metadata = new FabricModInspector().inspect(source).root();
        DeterministicJarPreparer preparer = new DeterministicJarPreparer();
        Path first = temporaryDirectory.resolve("first.jar");
        Path second = temporaryDirectory.resolve("second.jar");

        preparer.prepare(source, first, metadata, PreparationManifest.pinned("1.21.1", "52.1.0"));
        preparer.prepare(source, second, metadata, PreparationManifest.pinned("1.21.1", "52.1.0"));

        assertThat(Files.readAllBytes(first)).isEqualTo(Files.readAllBytes(second));
        try (JarFile jar = new JarFile(first.toFile())) {
            assertThat(jar.getEntry("META-INF/OLD.SF")).isNull();
            assertThat(jar.getEntry("META-INF/OLD.RSA")).isNull();
            assertThat(read(jar, "example/data.txt")).isEqualTo("preserved");
            assertThat(read(jar, "fabric.mod.json")).contains("fixture");
            assertThat(read(jar, "META-INF/mods.toml"))
                    .contains("modLoader=\"fabricbridge\"")
                    .contains("modId=\"fixture\"");
            assertThat(read(jar, "META-INF/loaderbridge.json"))
                    .contains("\"sourceLoader\": \"fabric\"")
                    .contains("\"minecraftVersion\": \"1.21.1\"");
            assertThat(read(jar, "pack.mcmeta"))
                    .contains("\"pack_format\": 34")
                    .contains("fixture resources");
        }
    }

    @Test
    void mapsFabricOnlyIdsAndEmitsDeterministicDependencyOrder() throws Exception {
        Path source = temporaryDirectory.resolve("fabric-id.jar");
        writeJar(source, Map.of("fabric.mod.json", """
                {
                  "schemaVersion": 1,
                  "id": "fabric-example",
                  "version": "1.0.0",
                  "depends": {
                    "z-library": "*",
                    "a_library": ">=1",
                    "fabricloader": ">=0.16"
                  }
                }
                """));
        var metadata = new FabricModInspector().inspect(source).root();
        Path output = temporaryDirectory.resolve("fabric-id-output.jar");

        new DeterministicJarPreparer().prepare(
                source, output, metadata, PreparationManifest.pinned("1.21.1", "52.1.0"));

        try (JarFile jar = new JarFile(output.toFile())) {
            String toml = read(jar, "META-INF/mods.toml");
            String hostId = DeterministicJarPreparer.hostModId("fabric-example");
            assertThat(hostId).matches("^[a-z][a-z0-9_]{1,63}$");
            assertThat(toml).contains("modId=\"" + hostId + "\"")
                    .contains("modId=\"a_library\"")
                    .contains("modId=\"" + DeterministicJarPreparer.hostModId("z-library") + "\"")
                    .doesNotContain("modId=\"fabricloader\"");
            assertThat(toml.indexOf("modId=\"a_library\""))
                    .isLessThan(toml.indexOf("modId=\""
                            + DeterministicJarPreparer.hostModId("z-library") + "\""));
        }
    }

    @Test
    void preservesAuthorSuppliedPackMetadata() throws Exception {
        Path source = temporaryDirectory.resolve("custom-pack.jar");
        writeJar(source, Map.of(
                "fabric.mod.json", "{\"schemaVersion\":1,\"id\":\"custom_pack\",\"version\":\"1\"}",
                "pack.mcmeta", "{\"pack\":{\"description\":\"author supplied\",\"pack_format\":34}}"));
        Path output = temporaryDirectory.resolve("custom-pack-output.jar");
        new DeterministicJarPreparer().prepare(source, output,
                new FabricModInspector().inspect(source).root(),
                PreparationManifest.pinned("1.21.1", "52.1.0"));

        try (JarFile jar = new JarFile(output.toFile())) {
            assertThat(read(jar, "pack.mcmeta")).contains("author supplied");
        }
    }

    @Test
    void embedsRuntimeMappingsForTheLoaderApiShim() throws Exception {
        Path source = temporaryDirectory.resolve("mapping-mod.jar");
        writeJar(source, Map.of(
                "fabric.mod.json", "{\"schemaVersion\":1,\"id\":\"mapping_mod\",\"version\":\"1\"}"));
        Path mappings = temporaryDirectory.resolve("intermediary-named.tiny");
        Files.writeString(mappings, "tiny\t2\t0\tintermediary\tnamed\n"
                + "c\tnet/minecraft/class_310\tnet/minecraft/client/Minecraft\n");
        Path output = temporaryDirectory.resolve("mapping-mod-output.jar");

        new DeterministicJarPreparer().prepare(source, output,
                new FabricModInspector().inspect(source).root(),
                PreparationManifest.pinned("1.21.1", "52.1.0"), mappings);

        try (JarFile jar = new JarFile(output.toFile())) {
            assertThat(read(jar, "META-INF/loaderbridge/mappings.tiny"))
                    .contains("intermediary", "net/minecraft/client/Minecraft");
        }
    }

    @Test
    void declaresUniversalFabricMixinConfigurationsInTheForgeManifest() throws Exception {
        Path source = temporaryDirectory.resolve("mixin-mod.jar");
        writeJar(source, Map.of(
                "fabric.mod.json", """
                        {"schemaVersion":1,"id":"mixin_mod","version":"1",
                         "mixins":["z.mixins.json","a.mixins.json"]}
                        """,
                "a.mixins.json", "{}",
                "z.mixins.json", "{}"));
        Path output = temporaryDirectory.resolve("mixin-mod-output.jar");

        new DeterministicJarPreparer().prepare(source, output,
                new FabricModInspector().inspect(source).root(),
                PreparationManifest.pinned("1.21.1", "52.1.0"));

        try (JarFile jar = new JarFile(output.toFile())) {
            Manifest manifest = jar.getManifest();
            assertThat(manifest).isNotNull();
            assertThat(manifest.getMainAttributes().getValue("MixinConfigs"))
                    .isEqualTo("a.mixins.json,z.mixins.json");
        }
    }

    @Test
    void remapsAndRegistersAccessWidenerWithoutChangingOriginalResource() throws Exception {
        Path source = temporaryDirectory.resolve("access-widener-mod.jar");
        String original = "accessWidener v1 intermediary\n"
                + "accessible class net/minecraft/class_1\n"
                + "mutable field net/minecraft/class_1 field_1 I\n";
        writeJar(source, Map.of(
                "fabric.mod.json", """
                        {"schemaVersion":1,"id":"aw_mod","version":"1",
                         "accessWidener":"fixture.accesswidener"}
                        """,
                "fixture.accesswidener", original));
        Path mappings = temporaryDirectory.resolve("aw-runtime.tiny");
        Files.writeString(mappings, "tiny\t2\t0\tintermediary\tnamed\n"
                + "c\tnet/minecraft/class_1\tnet/minecraft/Example\n"
                + "\tf\tI\tfield_1\tvalue\n");
        Path output = temporaryDirectory.resolve("access-widener-output.jar");

        new DeterministicJarPreparer().prepare(source, output,
                new FabricModInspector().inspect(source).root(),
                PreparationManifest.pinned("1.21.1", "52.1.0")
                        .namespaces("neutral", "official"), mappings);

        try (JarFile jar = new JarFile(output.toFile())) {
            assertThat(read(jar, "fixture.accesswidener")).isEqualTo(original);
            String generated = jar.getManifest().getMainAttributes()
                    .getValue("LoaderBridge-Access-Widener");
            assertThat(generated).startsWith("META-INF/loaderbridge/access-wideners/")
                    .endsWith(".accesswidener");
            assertThat(read(jar, generated)).contains(
                    "accessWidener\tv1\tofficial",
                    "accessible\tclass\tnet/minecraft/Example",
                    "mutable\tfield\tnet/minecraft/Example\tvalue\tI");
        }
    }

    @Test
    void createsSideScopedMixinWrapperWithoutChangingOriginalConfig() throws Exception {
        Path source = temporaryDirectory.resolve("client-mixin-mod.jar");
        String original = """
                {"required":true,"package":"fixture.mixin","plugin":"fixture.Plugin",
                 "refmap":"fixture.refmap.json",
                 "mixins":["CommonMixin"],"client":["ExistingClientMixin"],
                 "server":["ExistingServerMixin"]}
                """;
        writeJar(source, Map.of(
                "fabric.mod.json", """
                        {"schemaVersion":1,"id":"client_mixin","version":"1",
                         "mixins":[{"config":"client.mixins.json","environment":"client"}]}
                        """,
                "client.mixins.json", original,
                "fixture.refmap.json", """
                        {"mappings":{"fixture.mixin.CommonMixin":{
                          "method_1":"Lnet/minecraft/class_1;method_1()V"
                        }}}
                        """));
        Path output = temporaryDirectory.resolve("client-mixin-output.jar");
        Path mappings = temporaryDirectory.resolve("mixin-runtime.tiny");
        Files.writeString(mappings, "tiny\t2\t0\tintermediary\tnamed\n"
                + "c\tnet/minecraft/class_1\tnet/minecraft/Example\n"
                + "\tm\t()V\tmethod_1\trun\n");

        new DeterministicJarPreparer().prepare(source, output,
                new FabricModInspector().inspect(source).root(),
                PreparationManifest.pinned("1.21.1", "52.1.0"), mappings);

        try (JarFile jar = new JarFile(output.toFile())) {
            assertThat(read(jar, "client.mixins.json")).isEqualTo(original);
            String wrapper = jar.getManifest().getMainAttributes().getValue("MixinConfigs");
            assertThat(wrapper).startsWith("META-INF/loaderbridge/mixins/").endsWith(".json");
            var config = com.google.gson.JsonParser.parseString(read(jar, wrapper)).getAsJsonObject();
            assertThat(config.has("mixins")).isFalse();
            assertThat(config.has("server")).isFalse();
            assertThat(config.getAsJsonArray("client")).extracting(element -> element.getAsString())
                    .containsExactly("CommonMixin", "ExistingClientMixin");
            assertThat(config.get("plugin").getAsString()).isEqualTo("fixture.Plugin");
            String refmap = config.get("refmap").getAsString();
            assertThat(refmap).startsWith("META-INF/loaderbridge/mixins/")
                    .endsWith(".refmap.json");
            var refmapJson = com.google.gson.JsonParser.parseString(read(jar, refmap))
                    .getAsJsonObject().getAsJsonObject("mappings")
                    .getAsJsonObject("fixture.mixin.CommonMixin");
            assertThat(refmapJson.get("method_1").getAsString())
                    .isEqualTo("Lnet/minecraft/Example;run()V");
            assertThat(refmapJson.get("run()V").getAsString())
                    .isEqualTo("Lnet/minecraft/Example;run()V");
        }
    }

    @Test
    void discoversSingleMixinTargetForUnqualifiedRefmapEntries() throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "fixture/mixin/AccessorMixin", null, "java/lang/Object", null);
        var annotation = writer.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;", false);
        var values = annotation.visitArray("value");
        values.visit(null, Type.getObjectType("net/minecraft/class_1"));
        values.visitEnd();
        annotation.visitEnd();
        writer.visitEnd();
        Path source = temporaryDirectory.resolve("target-owner.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            JarEntry entry = new JarEntry("fixture/mixin/AccessorMixin.class");
            jar.putNextEntry(entry);
            jar.write(writer.toByteArray());
            jar.closeEntry();
        }
        var config = com.google.gson.JsonParser.parseString("""
                {"package":"fixture.mixin","mixins":["AccessorMixin"]}
                """).getAsJsonObject();

        try (JarFile jar = new JarFile(source.toFile())) {
            assertThat(DeterministicJarPreparer.mixinTargetOwners(jar, config))
                    .containsEntry("fixture/mixin/AccessorMixin", "net/minecraft/class_1")
                    .containsEntry("fixture.mixin.AccessorMixin", "net/minecraft/class_1");
        }
    }

    private static String read(JarFile jar, String name) throws IOException {
        try (var input = jar.getInputStream(jar.getJarEntry(name))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeJar(Path output, Map<String, String> entries) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(output))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                JarEntry jarEntry = new JarEntry(entry.getKey());
                jarEntry.setTime(System.currentTimeMillis());
                jar.putNextEntry(jarEntry);
                jar.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
        }
    }
}
