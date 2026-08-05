package dev.loaderbridge.fabric.api.tag.convention;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEnchantmentTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalFluidTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalStructureTags;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.junit.jupiter.api.Test;

class FabricConventionTagsContractTest {
    @Test
    void exposesTheOfficialConventionTagModuleAndClass() {
        var descriptor = new FabricConventionTagsBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion()).isEqualTo("2.12.0+c3656daa19-loaderbridge.2");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-convention-tags-v2", "2.12.0+c3656daa19");
        assertThat(descriptor.requiredModules()).containsExactly("fabric-lifecycle-events-bridge");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                "net.fabricmc.fabric.api.tag.FabricTagKey",
                "net.fabricmc.fabric.api.tag.convention.v2.TagUtil",
                "net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags",
                "net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags",
                "net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags",
                "net.fabricmc.fabric.api.tag.convention.v2.ConventionalFluidTags",
                "net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags",
                "net.fabricmc.fabric.api.tag.convention.v2.ConventionalEnchantmentTags",
                "net.fabricmc.fabric.api.tag.convention.v2.ConventionalStructureTags"));
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

    @Test
    void exposesEveryPinnedConventionTagRegistryWithExactPaths() {
        assertThat(ConventionalBiomeTags.class.getFields()).hasSize(79);
        assertThat(ConventionalBlockTags.class.getFields()).hasSize(118);
        assertThat(ConventionalItemTags.class.getFields()).hasSize(272);
        assertThat(ConventionalFluidTags.class.getFields()).hasSize(12);
        assertThat(ConventionalEntityTypeTags.class.getFields()).hasSize(5);
        assertThat(ConventionalEnchantmentTags.class.getFields()).hasSize(6);
        assertThat(ConventionalStructureTags.class.getFields()).hasSize(2);
        assertThat(ConventionalBlockTags.ORES.location().toString()).isEqualTo("c:ores");
        assertThat(ConventionalItemTags.IRON_INGOTS.location().toString())
                .isEqualTo("c:ingots/iron");
        assertThat(ConventionalFluidTags.WATER.location().toString()).isEqualTo("c:water");
        assertThat(ConventionalEntityTypeTags.BOSSES.location().toString()).isEqualTo("c:bosses");
        assertThat(ConventionalEnchantmentTags.INCREASE_BLOCK_DROPS.location().toString())
                .isEqualTo("c:increase_block_drops");
        assertThat(ConventionalStructureTags.HIDDEN_FROM_DISPLAYERS.location().toString())
                .isEqualTo("c:hidden_from_displayers");
        assertThat(TagUtil.C_TAG_NAMESPACE).isEqualTo("c");
        assertThat(TagUtil.FABRIC_TAG_NAMESPACE).isEqualTo("fabric");
    }

    @Test
    void computesFabricTagTranslationKeysAndFallbackNames() {
        TagKey<net.minecraft.world.item.Item> tag = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("c", "ingots/iron"));
        assertThat(FabricTagKeySupport.translationKey(tag)).isEqualTo("tag.item.c.ingots.iron");
        assertThat(FabricTagKeySupport.name(tag).getString()).isEqualTo("#c:ingots/iron");

        TagKey<net.minecraft.world.item.Item> moddedRegistryTag = TagKey.create(
                net.minecraft.resources.ResourceKey.createRegistryKey(
                        ResourceLocation.fromNamespaceAndPath("loaderbridge", "custom/registry")),
                ResourceLocation.fromNamespaceAndPath("example", "nested/tag"));
        assertThat(FabricTagKeySupport.translationKey(moddedRegistryTag))
                .isEqualTo("tag.loaderbridge.custom.registry.example.nested.tag");
    }

    @Test
    void packagesPinnedTagDataAndTranslationsRatherThanOnlyEmptyKeys() {
        assertThat(getClass().getResource("/data/c/tags/item/ingots/iron.json")).isNotNull();
        assertThat(getClass().getResource("/data/c/tags/block/ores/diamond.json")).isNotNull();
        assertThat(getClass().getResource("/data/c/tags/worldgen/biome/is_plains.json"))
                .isNotNull();
        assertThat(getClass().getResource(
                "/assets/fabric-convention-tags-v2/lang/en_us.json")).isNotNull();
    }
}
