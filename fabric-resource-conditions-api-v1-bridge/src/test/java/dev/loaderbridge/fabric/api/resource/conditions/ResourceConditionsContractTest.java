package dev.loaderbridge.fabric.api.resource.conditions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.google.gson.JsonObject;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.JsonOps;
import dev.loaderbridge.fabric.runtime.BridgeFabricLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceConditionsContractTest {
    @BeforeEach
    void configureLoader() {
        BridgeFabricLoader.getInstance().configureHost(
                EnvType.SERVER, Path.of("build/resource-condition-test"), "1.21.1", false);
        BridgeResourceConditions.beginServerResourceReload(FeatureFlags.DEFAULT_FLAGS);
    }

    @Test
    void providerPinsTheExactPublicContract() {
        var descriptor = new FabricResourceConditionsBridgeProvider().descriptor();
        assertThat(descriptor.contractVersion())
                .isEqualTo("fabric-resource-conditions-api-v1:4.3.0");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("4.3.0+8dc279b119-loaderbridge.1");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-resource-conditions-api-v1", "4.3.0+8dc279b119");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrder(
                "net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition",
                "net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType",
                "net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions");
    }

    @Test
    void implementsLogicalAndLoadedModConditions() {
        assertThat(ResourceConditions.alwaysTrue().test(null)).isTrue();
        assertThat(ResourceConditions.not(ResourceConditions.alwaysTrue()).test(null)).isFalse();
        assertThat(ResourceConditions.and().test(null)).isTrue();
        assertThat(ResourceConditions.or().test(null)).isFalse();
        assertThat(ResourceConditions.allModsLoaded("minecraft", "fabricloader").test(null)).isTrue();
        assertThat(ResourceConditions.allModsLoaded("missing").test(null)).isFalse();
        assertThat(ResourceConditions.anyModsLoaded("missing", "minecraft").test(null)).isTrue();
    }

    @Test
    void dispatchCodecParsesPinnedFabricJsonShape() {
        JsonObject json = new JsonObject();
        json.addProperty("condition", "fabric:all_mods_loaded");
        var values = new com.google.gson.JsonArray();
        values.add("minecraft");
        json.add("values", values);

        ResourceCondition condition = ResourceCondition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertThat(condition.test(null)).isTrue();
        assertThat(condition.getType().id()).isEqualTo(
                ResourceLocation.fromNamespaceAndPath("fabric", "all_mods_loaded"));
    }

    @Test
    void dispatchCodecPreservesAnyLoadedSemanticsAndAcceptsConditionLists() {
        JsonObject any = new JsonObject();
        any.addProperty("condition", "fabric:any_mods_loaded");
        var values = new com.google.gson.JsonArray();
        values.add("missing_one");
        values.add("minecraft");
        any.add("values", values);

        ResourceCondition parsedAny = ResourceCondition.CODEC.parse(JsonOps.INSTANCE, any).getOrThrow();
        assertThat(parsedAny.test(null)).isTrue();

        var list = new com.google.gson.JsonArray();
        list.add(any);
        JsonObject missing = new JsonObject();
        missing.addProperty("condition", "fabric:all_mods_loaded");
        var missingValues = new com.google.gson.JsonArray();
        missingValues.add("missing_two");
        missing.add("values", missingValues);
        list.add(missing);

        ResourceCondition parsedList = ResourceCondition.CONDITION_CODEC
                .parse(JsonOps.INSTANCE, list)
                .getOrThrow();
        assertThat(parsedList.test(null)).isFalse();
    }

    @Test
    void rejectsDuplicateCustomConditionTypes() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("fixture", "duplicate");
        ResourceConditionType<ResourceCondition> type = ResourceConditionType.create(
                id, com.mojang.serialization.MapCodec.unit(ResourceConditions::alwaysTrue));
        ResourceConditions.register(type);

        assertThatIllegalArgumentException().isThrownBy(() -> ResourceConditions.register(type))
                .withMessageContaining("Duplicate resource condition")
                .withMessageContaining(id.toString());
    }

    @Test
    void removesRejectedJsonResourcesBeforeVanillaParsesThem() {
        JsonObject rejected = new JsonObject();
        rejected.addProperty("type", "missing:serializer");
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "fabric:all_mods_loaded");
        var values = new com.google.gson.JsonArray();
        values.add("missing_mod");
        condition.add("values", values);
        var conditions = new com.google.gson.JsonArray();
        conditions.add(condition);
        rejected.add(ResourceConditions.CONDITIONS_KEY, conditions);

        assertThat(BridgeResourceConditions.test(rejected, null)).isFalse();
    }

    @Test
    void evaluatesTagsCapturedDuringTheCurrentReload() {
        ResourceLocation tagId = ResourceLocation.fromNamespaceAndPath("fixture", "available");
        TagManager.LoadResult<Item> result = new TagManager.LoadResult<>(
                Registries.ITEM, Map.of(tagId, List.of()));
        BridgeResourceConditions.captureLoadedTags(List.of(result));

        assertThat(ResourceConditions.tagsPopulated(TagKey.create(Registries.ITEM, tagId)).test(null))
                .isTrue();
        assertThat(ResourceConditions.tagsPopulated(TagKey.create(
                        Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath("fixture", "missing")))
                .test(null)).isFalse();
    }

    @Test
    void evaluatesSelectedFeatureFlagsAndRegistryEntries() {
        BridgeResourceConditions.beginServerResourceReload(FeatureFlagSet.of());
        assertThat(ResourceConditions.featuresEnabled(FeatureFlags.VANILLA).test(null)).isFalse();
        BridgeResourceConditions.beginServerResourceReload(FeatureFlags.DEFAULT_FLAGS);
        assertThat(ResourceConditions.featuresEnabled(FeatureFlags.VANILLA).test(null)).isTrue();

        ResourceKey<Registry<String>> registryKey = ResourceKey.createRegistryKey(
                ResourceLocation.fromNamespaceAndPath("fixture", "strings"));
        ResourceKey<String> present = ResourceKey.create(
                registryKey, ResourceLocation.fromNamespaceAndPath("fixture", "present"));
        HolderLookup.RegistryLookup<String> registry = lookupContaining(registryKey, present);
        HolderLookup.Provider lookup = HolderLookup.Provider.create(Stream.of(registry));
        assertThat(ResourceConditions.registryContains(registryKey, present.location()).test(lookup))
                .isTrue();
    }

    private static <T> HolderLookup.RegistryLookup<T> lookupContaining(
            ResourceKey<Registry<T>> registryKey, ResourceKey<T> present) {
        return new HolderLookup.RegistryLookup<>() {
            private final Holder.Reference<T> holder = Holder.Reference.createStandAlone(this, present);

            @Override public ResourceKey<? extends Registry<? extends T>> key() { return registryKey; }
            @Override public Lifecycle registryLifecycle() { return Lifecycle.stable(); }
            @Override public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
                return key.equals(present) ? Optional.of(holder) : Optional.empty();
            }
            @Override public Optional<HolderSet.Named<T>> get(TagKey<T> tag) { return Optional.empty(); }
            @Override public Stream<Holder.Reference<T>> listElements() { return Stream.of(holder); }
            @Override public Stream<HolderSet.Named<T>> listTags() { return Stream.empty(); }
        };
    }
}
