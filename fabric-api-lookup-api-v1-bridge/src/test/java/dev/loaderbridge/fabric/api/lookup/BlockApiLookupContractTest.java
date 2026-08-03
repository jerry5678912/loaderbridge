package dev.loaderbridge.fabric.api.lookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.custom.ApiLookupMap;
import net.fabricmc.fabric.api.lookup.v1.custom.ApiProviderMap;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BlockApiLookupContractTest {
    @Test
    void providerPinsOnlyTheImplementedBlockLookupSurface() {
        var descriptor = new FabricApiLookupBridgeProvider().descriptor();

        assertThat(descriptor.contractVersion()).isEqualTo("fabric-api-lookup-api-v1:1.6.72");
        assertThat(descriptor.implementationVersion()).isEqualTo("1.6.72+d30f6a7919-loaderbridge.2");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrder(
                "net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup",
                "net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup$BlockApiProvider",
                "net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup$BlockEntityApiProvider",
                "net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache",
                "net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup",
                "net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup$ItemApiProvider",
                "net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup",
                "net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup$EntityApiProvider",
                "net.fabricmc.fabric.api.lookup.v1.custom.ApiLookupMap",
                "net.fabricmc.fabric.api.lookup.v1.custom.ApiLookupMap$LookupConstructor",
                "net.fabricmc.fabric.api.lookup.v1.custom.ApiLookupMap$LookupFactory",
                "net.fabricmc.fabric.api.lookup.v1.custom.ApiProviderMap");
    }

    @Test
    void lookupIdentityAndTypeSafetyMatchThePublicContract() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "string_lookup");
        BlockApiLookup<String, Void> first = BlockApiLookup.get(id, String.class, Void.class);
        BlockApiLookup<String, Void> second = BlockApiLookup.get(id, String.class, Void.class);

        assertThat(second).isSameAs(first);
        assertThat(first.getId()).isEqualTo(id);
        assertThat(first.apiClass()).isEqualTo(String.class);
        assertThat(first.contextClass()).isEqualTo(Void.class);
        assertThatThrownBy(() -> BlockApiLookup.get(id, Integer.class, Void.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockProviderRegistrationRejectsAnEmptyTargetSet() {
        var lookup = BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "block_provider"), String.class, Void.class);
        assertThatThrownBy(() -> lookup.registerForBlocks(
                (world, pos, state, blockEntity, context) -> "value"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesThePinnedCacheContract() throws ReflectiveOperationException {
        assertThat(BlockApiCache.class.getDeclaredMethod("find", Object.class).getReturnType())
                .isEqualTo(Object.class);
        assertThat(BlockApiCache.class.getDeclaredMethod("getLookup").getReturnType())
                .isEqualTo(BlockApiLookup.class);
    }

    @Test
    void itemAndEntityLookupsAreUniqueAndTypeSafe() {
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "item_lookup");
        ResourceLocation entityId = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "entity_lookup");

        assertThat(ItemApiLookup.get(itemId, String.class, Void.class))
                .isSameAs(ItemApiLookup.get(itemId, String.class, Void.class));
        assertThat(EntityApiLookup.get(entityId, String.class, Void.class))
                .isSameAs(EntityApiLookup.get(entityId, String.class, Void.class));
        assertThatThrownBy(() -> ItemApiLookup.get(itemId, Integer.class, Void.class))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntityApiLookup.get(entityId, Integer.class, Void.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customMapsPreserveLookupTypesAndIdentityKeys() {
        ApiLookupMap<String> lookups = ApiLookupMap.create(
                (id, apiClass, contextClass) -> id.toString());
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "custom_lookup");
        assertThat(lookups.getLookup(id, String.class, Void.class)).isEqualTo(id.toString());
        assertThat(lookups).containsExactly(id.toString());
        assertThatThrownBy(() -> lookups.getLookup(id, Integer.class, Void.class))
                .isInstanceOf(IllegalArgumentException.class);

        ApiProviderMap<Object, String> providers = ApiProviderMap.create();
        Object first = new String("key");
        Object equalButDistinct = new String("key");
        assertThat(providers.putIfAbsent(first, "value")).isNull();
        assertThat(providers.get(first)).isEqualTo("value");
        assertThat(providers.get(equalButDistinct)).isNull();
    }
}
