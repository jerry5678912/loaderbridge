package dev.loaderbridge.fabric.api.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.serialization.Lifecycle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistryView;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RegistrySyncContractTest {
    @Test
    void providerAdvertisesOnlyImplementedPublicSurface() {
        var descriptor = new FabricRegistrySyncBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion())
                .isEqualTo("5.1.3+60c3209b19-loaderbridge.4");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                "net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder",
                "net.fabricmc.fabric.api.event.registry.DynamicRegistries",
                "net.fabricmc.fabric.api.event.registry.DynamicRegistries$SyncOption",
                "net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback",
                "net.fabricmc.fabric.api.event.registry.DynamicRegistryView",
                "net.fabricmc.fabric.api.event.registry.RegistryAttribute",
                "net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder",
                "net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback",
                "net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback",
                "net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback$RemapState"));
    }

    @Test
    void attributesAreIsolatedByRegistryKey() {
        ResourceKey<Registry<String>> first = registryKey("first");
        ResourceKey<Registry<String>> second = registryKey("second");
        RegistryAttributeHolder.get(first).addAttribute(RegistryAttribute.SYNCED);
        assertThat(RegistryAttributeHolder.get(first).hasAttribute(RegistryAttribute.SYNCED)).isTrue();
        assertThat(RegistryAttributeHolder.get(second).hasAttribute(RegistryAttribute.SYNCED)).isFalse();
    }

    @Test
    void entryAddedEventIsStableAndReceivesRegisteredValue() {
        ResourceKey<Registry<String>> key = registryKey("events");
        MappedRegistry<String> registry = new MappedRegistry<>(key, Lifecycle.stable());
        AtomicReference<String> observed = new AtomicReference<>();
        RegistryEntryAddedCallback.event(registry)
                .register((rawId, id, value) -> observed.set(rawId + ":" + id + ":" + value));

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("loaderbridge", "value");
        registry.register(ResourceKey.create(key, id), "payload", RegistrationInfo.BUILT_IN);
        RegistryEventDispatcher.fireEntryAdded(registry, registry.getId("payload"), id, "payload");

        assertThat(observed).hasValue("0:loaderbridge:value:payload");
        assertThat(RegistryEntryAddedCallback.event(registry))
                .isSameAs(RegistryEntryAddedCallback.event(registry));
    }

    @Test
    void builderFactoriesExposeSimpleAndDefaultedContracts() {
        ResourceKey<Registry<String>> key = registryKey("builder");
        MappedRegistry<String> registry = new MappedRegistry<>(key, Lifecycle.stable());
        assertThat(FabricRegistryBuilder.from(registry).attribute(RegistryAttribute.SYNCED)).isNotNull();
        assertThat(FabricRegistryBuilder.createSimple(key)).isNotNull();
        assertThat(FabricRegistryBuilder.createDefaulted(key,
                ResourceLocation.fromNamespaceAndPath("loaderbridge_test", "default"))).isNotNull();
    }

    @Test
    void dynamicRegistryRegistrationSurfaceMatchesPinnedContract() throws Exception {
        assertThat(DynamicRegistries.SyncOption.values())
                .containsExactly(DynamicRegistries.SyncOption.SKIP_WHEN_EMPTY);
        assertThat(DynamicRegistries.class.getMethod("register",
                ResourceKey.class, com.mojang.serialization.Codec.class)).isNotNull();
        assertThat(DynamicRegistries.class.getMethod("registerSynced",
                ResourceKey.class, com.mojang.serialization.Codec.class,
                DynamicRegistries.SyncOption[].class)).isNotNull();
        assertThat(DynamicRegistries.class.getMethod("registerSynced",
                ResourceKey.class, com.mojang.serialization.Codec.class,
                com.mojang.serialization.Codec.class,
                DynamicRegistries.SyncOption[].class)).isNotNull();
    }

    @Test
    void dynamicSetupSurfaceMatchesPinnedContract() throws Exception {
        assertThat(DynamicRegistrySetupCallback.EVENT).isNotNull();
        assertThat(DynamicRegistrySetupCallback.class.getMethod(
                "onRegistrySetup", DynamicRegistryView.class)).isNotNull();
        assertThat(DynamicRegistryView.class.getMethod("asDynamicRegistryManager")).isNotNull();
        assertThat(DynamicRegistryView.class.getMethod("stream")).isNotNull();
        assertThat(DynamicRegistryView.class.getMethod("getOptional", ResourceKey.class)).isNotNull();
        assertThat(DynamicRegistryView.class.getMethod("registerEntryAdded",
                ResourceKey.class, RegistryEntryAddedCallback.class)).isNotNull();
    }

    @Test
    void skipWhenEmptyTracksOnlyRegistriesUsingTheOption() {
        ResourceKey<Registry<String>> skipped = registryKey("skipped_empty");
        ResourceKey<Registry<String>> ordinary = registryKey("ordinary_empty");
        DynamicRegistrySyncOptions.markSkipWhenEmpty(skipped);

        MappedRegistry<String> skippedRegistry =
                new MappedRegistry<>(skipped, Lifecycle.stable());
        MappedRegistry<String> ordinaryRegistry =
                new MappedRegistry<>(ordinary, Lifecycle.stable());
        assertThat(DynamicRegistrySyncOptions.shouldSkipEmpty(skipped, skippedRegistry)).isTrue();
        assertThat(DynamicRegistrySyncOptions.shouldSkipEmpty(ordinary, ordinaryRegistry)).isFalse();
        skippedRegistry.register(ResourceKey.create(skipped,
                        ResourceLocation.fromNamespaceAndPath("loaderbridge_test", "value")),
                "value", RegistrationInfo.BUILT_IN);
        assertThat(DynamicRegistrySyncOptions.shouldSkipEmpty(skipped, skippedRegistry)).isFalse();
    }

    private static ResourceKey<Registry<String>> registryKey(String path) {
        return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("loaderbridge_test", path));
    }
}
