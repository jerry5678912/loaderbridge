package dev.loaderbridge.fabric.api.tag.convention;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import org.junit.jupiter.api.Test;

class FabricConventionTagsContractTest {
    @Test
    void exposesTheOfficialConventionTagModuleAndClass() {
        var descriptor = new FabricConventionTagsBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion()).isEqualTo("2.12.0+c3656daa19-loaderbridge.1");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-convention-tags-v2", "2.12.0+c3656daa19");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                "net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags"));
    }

    @Test
    void biomeTagsUseFabricSharedCNamespaceAndExactPaths() {
        assertThat(ConventionalBiomeTags.IS_PLAINS.location().toString()).isEqualTo("c:is_plains");
        assertThat(ConventionalBiomeTags.IS_FLOWER_FOREST.location().toString())
                .isEqualTo("c:is_flower_forest");
        assertThat(ConventionalBiomeTags.IS_HOT_OVERWORLD.location().toString())
                .isEqualTo("c:is_hot/overworld");
        assertThat(ConventionalBiomeTags.IS_CONIFEROUS_TREE.location().toString())
                .isEqualTo("c:is_tree/coniferous");
    }
}
