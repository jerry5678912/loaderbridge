package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MixinRefmapTransformerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void translatesMethodsFieldsDescriptorsAndAddsRuntimeSelectorKeys() throws Exception {
        TinyMappingIndex mappings = mappings();
        byte[] translated = new MixinRefmapTransformer().transform("""
                {
                  "mappings": {
                    "fixture.ServerMixin": {
                      "method_1": "Lnet/minecraft/class_1;method_1(Lnet/minecraft/class_2;)V",
                      "field_1": "Lnet/minecraft/class_1;field_1:Lnet/minecraft/class_2;"
                    }
                  },
                  "data": {
                    "named:intermediary": {
                      "fixture.ServerMixin": {
                        "method_1": "Lnet/minecraft/class_1;method_1(Lnet/minecraft/class_2;)V"
                      }
                    }
                  }
                }
                """.getBytes(StandardCharsets.UTF_8), mappings, "fixture.refmap.json");

        var root = JsonParser.parseString(new String(translated, StandardCharsets.UTF_8))
                .getAsJsonObject();
        var values = root.getAsJsonObject("mappings")
                .getAsJsonObject("fixture.ServerMixin");
        assertThat(values.get("method_1").getAsString())
                .isEqualTo("Lnet/minecraft/Example;run(Lnet/minecraft/Argument;)V");
        assertThat(values.get("run(Lnet/minecraft/Argument;)V").getAsString())
                .isEqualTo("Lnet/minecraft/Example;run(Lnet/minecraft/Argument;)V");
        assertThat(values.get("field_1").getAsString())
                .isEqualTo("Lnet/minecraft/Example;argument:Lnet/minecraft/Argument;");
        assertThat(values.get("argument:Lnet/minecraft/Argument;").getAsString())
                .isEqualTo("Lnet/minecraft/Example;argument:Lnet/minecraft/Argument;");
        assertThat(root.getAsJsonObject("data").getAsJsonObject("named:intermediary")
                .getAsJsonObject("fixture.ServerMixin")
                .get("run(Lnet/minecraft/Argument;)V").getAsString())
                .contains("net/minecraft/Example;run");
    }

    @Test
    void rejectsTranslatedKeyCollisions() throws Exception {
        assertThatThrownBy(() -> new MixinRefmapTransformer().transform("""
                {"mappings":{"fixture.Mixin":{
                  "method_1":"Lnet/minecraft/class_1;method_1()V",
                  "run()V":"different"
                }}}
                """.getBytes(StandardCharsets.UTF_8), mappings(), "collision.refmap.json"))
                .hasMessageContaining("LB-MIXIN-REFMAP-003");
    }

    @Test
    void translatesUnqualifiedAccessorEntriesUsingTheMixinTarget() throws Exception {
        byte[] translated = new MixinRefmapTransformer().transform("""
                {"mappings":{"fixture/ServerMixin":{
                  "argument":"field_1:Lnet/minecraft/class_2;",
                  "run":"method_1(Lnet/minecraft/class_2;)V"
                }}}
                """.getBytes(StandardCharsets.UTF_8), mappings(), "accessor.refmap.json",
                Map.of("fixture/ServerMixin", "net/minecraft/Example"));

        var values = JsonParser.parseString(new String(translated, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("mappings")
                .getAsJsonObject("fixture/ServerMixin");
        assertThat(values.get("argument").getAsString())
                .isEqualTo("argument:Lnet/minecraft/Argument;");
        assertThat(values.get("run").getAsString())
                .isEqualTo("run(Lnet/minecraft/Argument;)V");
    }

    @Test
    void translatesIntermediaryMemberNamesAfterTinyRemapperAlreadyMappedDescriptors() throws Exception {
        byte[] translated = new MixinRefmapTransformer().transform("""
                {"mappings":{"fixture/BoatMixin":{
                  "fall":"Lnet/minecraft/Example;method_1(Lnet/minecraft/Argument;)V"
                }}}
                """.getBytes(StandardCharsets.UTF_8), mappings(), "boat.refmap.json");

        var values = JsonParser.parseString(new String(translated, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("mappings")
                .getAsJsonObject("fixture/BoatMixin");
        assertThat(values.get("fall").getAsString())
                .isEqualTo("Lnet/minecraft/Example;run(Lnet/minecraft/Argument;)V");
        assertThat(values.get("run(Lnet/minecraft/Argument;)V").getAsString())
                .isEqualTo("Lnet/minecraft/Example;run(Lnet/minecraft/Argument;)V");
    }

    @Test
    void translatesInheritedIntermediarySelectorAgainstRuntimeSubclassOwner() throws Exception {
        byte[] translated = new MixinRefmapTransformer().transform("""
                {"mappings":{"fixture/ChildMixin":{
                  "fall":"Lnet/minecraft/ExampleChild;method_1(Lnet/minecraft/Argument;)V"
                }}}
                """.getBytes(StandardCharsets.UTF_8), mappings(), "child.refmap.json");

        var value = JsonParser.parseString(new String(translated, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("mappings")
                .getAsJsonObject("fixture/ChildMixin").get("fall").getAsString();
        assertThat(value).isEqualTo(
                "Lnet/minecraft/ExampleChild;run(Lnet/minecraft/Argument;)V");
    }

    private TinyMappingIndex mappings() throws Exception {
        Path mappings = temporaryDirectory.resolve("mappings.tiny");
        Files.writeString(mappings, "tiny\t2\t0\tintermediary\tnamed\n"
                + "c\tnet/minecraft/class_1\tnet/minecraft/Example\n"
                + "\tf\tLnet/minecraft/class_2;\tfield_1\targument\n"
                + "\tm\t(Lnet/minecraft/class_2;)V\tmethod_1\trun\n"
                + "\tm\t()V\tmethod_1\trun\n"
                + "c\tnet/minecraft/class_2\tnet/minecraft/Argument\n"
                + "c\tnet/minecraft/class_3\tnet/minecraft/ExampleChild\n");
        return TinyMappingIndex.read(mappings);
    }
}
