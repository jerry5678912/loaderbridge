package dev.loaderbridge.fabric.api.object.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.datafixers.types.Type;
import java.lang.reflect.Modifier;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
class FabricObjectBuilderContractTest {
    @Test
    void providerPinsOnlyImplementedOfficialPublicTypes() {
        var descriptor = new FabricObjectBuilderBridgeProvider().descriptor();

        assertThat(descriptor.contractVersion()).isEqualTo("fabric-object-builder-api-v1:15.2.1");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("15.2.1+40875a9319-loaderbridge.1");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrder(
                "net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder",
                "net.fabricmc.fabric.api.object.builder.v1.block.entity."
                        + "FabricBlockEntityTypeBuilder$Factory",
                "net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry");
    }

    @Test
    void exposesThePinnedBinaryBuilderContract() throws ReflectiveOperationException {
        Class<?> builder = FabricBlockEntityTypeBuilder.class;

        assertThat(Modifier.isFinal(builder.getModifiers())).isTrue();
        assertThat(builder.getDeclaredMethod("create",
                FabricBlockEntityTypeBuilder.Factory.class, Block[].class).getReturnType())
                .isEqualTo(FabricBlockEntityTypeBuilder.class);
        assertThat(builder.getDeclaredMethod("addBlock", Block.class).getReturnType())
                .isEqualTo(FabricBlockEntityTypeBuilder.class);
        assertThat(builder.getDeclaredMethod("addBlocks", Block[].class).getReturnType())
                .isEqualTo(FabricBlockEntityTypeBuilder.class);
        assertThat(builder.getDeclaredMethod("build").getReturnType())
                .isEqualTo(BlockEntityType.class);
        assertThat(builder.getDeclaredMethod("build", Type.class).getReturnType())
                .isEqualTo(BlockEntityType.class);
    }

    @Test
    void exposesBothPinnedDefaultAttributeRegistrationOverloads()
            throws ReflectiveOperationException {
        assertThat(FabricDefaultAttributeRegistry.class.getDeclaredMethod(
                "register", EntityType.class, AttributeSupplier.Builder.class)).isNotNull();
        assertThat(FabricDefaultAttributeRegistry.class.getDeclaredMethod(
                "register", EntityType.class, AttributeSupplier.class)).isNotNull();
    }
}
