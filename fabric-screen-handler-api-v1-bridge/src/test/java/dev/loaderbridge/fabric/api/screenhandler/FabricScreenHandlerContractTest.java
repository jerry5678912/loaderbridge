package dev.loaderbridge.fabric.api.screenhandler;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

class FabricScreenHandlerContractTest {
    @Test
    void advertisesTheExactPinnedPublicContractAndDependencies() {
        var descriptor = new FabricScreenHandlerBridgeProvider().descriptor();

        assertThat(descriptor.contractVersion())
                .isEqualTo("fabric-screen-handler-api-v1:1.3.91");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("1.3.91+b559734419-loaderbridge.1");
        assertThat(descriptor.providedModVersions())
                .containsExactlyEntriesOf(java.util.Map.of(
                        "fabric-screen-handler-api-v1", "1.3.91+b559734419"));
        assertThat(descriptor.requiredModules())
                .containsExactlyInAnyOrder("fabric-api-base-bridge", "fabric-networking-api-v1-bridge");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                ExtendedScreenHandlerFactory.class.getName(),
                ExtendedScreenHandlerType.class.getName(),
                ExtendedScreenHandlerType.ExtendedFactory.class.getName(),
                FabricScreenHandlerFactory.class.getName()));
    }

    @Test
    void publicTypesMatchThePinnedAbiShape() throws Exception {
        assertThat(MenuProvider.class.isAssignableFrom(ExtendedScreenHandlerFactory.class)).isTrue();
        assertThat(ExtendedScreenHandlerType.class.getDeclaredConstructors()).hasSize(1);
        assertThat(ExtendedScreenHandlerType.class.getDeclaredMethod(
                "create", int.class, Inventory.class, Object.class)).isNotNull();
        assertThat(ExtendedScreenHandlerType.class.getDeclaredMethod("getPacketCodec").getReturnType())
                .isEqualTo(StreamCodec.class);
        assertThat(ExtendedScreenHandlerType.ExtendedFactory.class.isInterface()).isTrue();
        assertThat(Modifier.isPublic(ExtendedScreenHandlerType.ExtendedFactory.class.getModifiers())).isTrue();
    }

    @Test
    void factoryDefaultMatchesFabric() {
        FabricScreenHandlerFactory factory = new FabricScreenHandlerFactory() { };
        assertThat(factory.shouldCloseCurrentScreen()).isTrue();
    }

    @Test
    void forgeManifestRequiresNetworkingWithoutMisorderingFabricSemanticVersions()
            throws Exception {
        try (var stream = getClass().getResourceAsStream("/META-INF/mods.toml")) {
            assertThat(stream).isNotNull();
            String manifest = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(manifest).contains("modId=\"loaderbridge_fabric_networking\"")
                    .contains("versionRange=\"[0,)\"");
            assertThat(manifest).doesNotContain("versionRange=\"[4.3.1.5,)\"");
        }
    }
}
