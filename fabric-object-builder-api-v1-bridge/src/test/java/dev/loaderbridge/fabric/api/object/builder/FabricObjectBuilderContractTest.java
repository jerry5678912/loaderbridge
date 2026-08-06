package dev.loaderbridge.fabric.api.object.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.datafixers.types.Type;
import java.lang.reflect.Modifier;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.MinecartComparatorLogic;
import net.fabricmc.fabric.api.object.builder.v1.entity.MinecartComparatorLogicRegistry;
import net.fabricmc.fabric.api.object.builder.v1.villager.VillagerProfessionBuilder;
import net.fabricmc.fabric.api.object.builder.v1.villager.VillagerTypeHelper;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
class FabricObjectBuilderContractTest {
    @Test
    void providerPinsOnlyImplementedOfficialPublicTypes() {
        var descriptor = new FabricObjectBuilderBridgeProvider().descriptor();

        assertThat(descriptor.contractVersion()).isEqualTo("fabric-object-builder-api-v1:15.2.1");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("15.2.1+40875a9319-loaderbridge.10");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrder(
                "net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings",
                "net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder",
                "net.fabricmc.fabric.api.object.builder.v1.block.entity."
                        + "FabricBlockEntityTypeBuilder$Factory",
                "net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType",
                "net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType$Builder",
                "net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry",
                "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder",
                "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder$Living",
                "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder$Mob",
                "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType",
                "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType$Builder",
                "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType$Builder$Living",
                "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType$Builder$Mob",
                "net.fabricmc.fabric.api.object.builder.v1.entity.MinecartComparatorLogic",
                "net.fabricmc.fabric.api.object.builder.v1.entity.MinecartComparatorLogicRegistry",
                "net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder",
                "net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder",
                "net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper",
                "net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper$VillagerOffersAdder",
                "net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper$WanderingTraderOffersBuilder",
                "net.fabricmc.fabric.api.object.builder.v1.villager.VillagerProfessionBuilder",
                "net.fabricmc.fabric.api.object.builder.v1.villager.VillagerTypeHelper",
                "net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper");
    }

    @Test
    void exposesDeprecatedFabricBlockSettingsContract() throws ReflectiveOperationException {
        assertThat(FabricBlockSettings.class.getSuperclass())
                .isEqualTo(net.minecraft.world.level.block.state.BlockBehaviour.Properties.class);
        assertThat(FabricBlockSettings.class.getDeclaredMethod("create").getReturnType())
                .isEqualTo(FabricBlockSettings.class);
        assertThat(FabricBlockSettings.class.getDeclaredMethod("copyOf",
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.class)
                .getReturnType()).isEqualTo(FabricBlockSettings.class);
        assertThat(FabricBlockSettings.class.getDeclaredMethod(
                "luminance", int.class).getReturnType()).isEqualTo(FabricBlockSettings.class);
        assertThat(FabricBlockSettings.class.getDeclaredMethod(
                "collidable", boolean.class).getReturnType()).isEqualTo(FabricBlockSettings.class);
    }

    @Test
    void exposesMinecartComparatorRegistryContract() throws ReflectiveOperationException {
        assertThat(MinecartComparatorLogicRegistry.class.getDeclaredMethod(
                "register", EntityType.class, MinecartComparatorLogic.class)).isNotNull();
        assertThat(MinecartComparatorLogicRegistry.class.getDeclaredMethod(
                "getCustomComparatorLogic", EntityType.class)).isNotNull();
    }

    @Test
    void exposesVillagerAndPointOfInterestContracts() throws ReflectiveOperationException {
        assertThat(VillagerProfessionBuilder.class.getDeclaredMethod("create").getReturnType())
                .isEqualTo(VillagerProfessionBuilder.class);
        assertThat(VillagerTypeHelper.class.getDeclaredMethod(
                "register", ResourceLocation.class)).isNotNull();
        assertThat(PointOfInterestHelper.class.getDeclaredMethod("register",
                ResourceLocation.class, int.class, int.class, Block[].class)).isNotNull();
        assertThat(PointOfInterestHelper.class.getDeclaredMethod("register",
                ResourceLocation.class, int.class, int.class, Iterable.class)).isNotNull();
    }

    @Test
    void exposesInjectedBlockEntityTypeExtensions() throws ReflectiveOperationException {
        assertThat(FabricBlockEntityType.class.getDeclaredMethod("addSupportedBlock", Block.class))
                .isNotNull();
        assertThat(FabricBlockEntityType.Builder.class.getDeclaredMethod("build").getReturnType())
                .isEqualTo(BlockEntityType.class);
    }

    @Test
    void exposesBlockSetBuilderContract() throws ReflectiveOperationException {
        assertThat(BlockSetTypeBuilder.class.getDeclaredMethod("copyOf", BlockSetType.class))
                .isNotNull();
        assertThat(BlockSetTypeBuilder.class.getDeclaredMethod("register", ResourceLocation.class)
                .getReturnType()).isEqualTo(BlockSetType.class);
        assertThat(BlockSetTypeBuilder.class.getDeclaredMethod("build", ResourceLocation.class)
                .getReturnType()).isEqualTo(BlockSetType.class);
    }

    @Test
    void exposesWoodTypeBuilderContract() throws ReflectiveOperationException {
        assertThat(WoodTypeBuilder.class.getDeclaredMethod("copyOf", WoodType.class)).isNotNull();
        assertThat(WoodTypeBuilder.class.getDeclaredMethod("register", ResourceLocation.class,
                BlockSetType.class).getReturnType()).isEqualTo(WoodType.class);
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

    @Test
    void exposesThePinnedDeprecatedEntityBuilderContract() throws ReflectiveOperationException {
        Class<?> builder = FabricEntityTypeBuilder.class;

        assertThat(builder.getDeclaredMethod("create").getReturnType()).isEqualTo(builder);
        assertThat(builder.getDeclaredMethod("create", MobCategory.class).getReturnType())
                .isEqualTo(builder);
        assertThat(builder.getDeclaredMethod("create", MobCategory.class,
                EntityType.EntityFactory.class).getReturnType()).isEqualTo(builder);
        assertThat(builder.getDeclaredMethod("createLiving").getReturnType())
                .isEqualTo(FabricEntityTypeBuilder.Living.class);
        assertThat(builder.getDeclaredMethod("createMob").getReturnType())
                .isEqualTo(FabricEntityTypeBuilder.Mob.class);
        assertThat(builder.getDeclaredMethod("spawnGroup", MobCategory.class).getReturnType())
                .isEqualTo(builder);
        assertThat(builder.getDeclaredMethod("entityFactory", EntityType.EntityFactory.class)
                .getReturnType()).isEqualTo(builder);
        assertThat(builder.getDeclaredMethod("dimensions", EntityDimensions.class).getReturnType())
                .isEqualTo(builder);
        assertThat(builder.getDeclaredMethod("trackRangeChunks", int.class).getReturnType())
                .isEqualTo(builder);
        assertThat(builder.getDeclaredMethod("forceTrackedVelocityUpdates", boolean.class)
                .getReturnType()).isEqualTo(builder);
        assertThat(builder.getDeclaredMethod("build").getReturnType()).isEqualTo(EntityType.class);
    }

    @Test
    void exposesLivingAndMobBuilderSpecializations() throws ReflectiveOperationException {
        assertThat(FabricEntityTypeBuilder.Living.class.getDeclaredMethod(
                "defaultAttributes", java.util.function.Supplier.class).getReturnType())
                .isEqualTo(FabricEntityTypeBuilder.Living.class);
        assertThat(FabricEntityTypeBuilder.Living.class.getTypeParameters()[0]
                .getBounds()[0].getTypeName()).isEqualTo(LivingEntity.class.getName());
        assertThat(FabricEntityTypeBuilder.Mob.class.getTypeParameters()[0]
                .getBounds()[0].getTypeName()).isEqualTo(Mob.class.getName());
    }

    @Test
    void exposesThePinnedModernEntityBuilderExtensions() throws ReflectiveOperationException {
        assertThat(FabricEntityType.Builder.class.getDeclaredMethod(
                "alwaysUpdateVelocity", boolean.class).getReturnType())
                .isEqualTo(EntityType.Builder.class);
        assertThat(FabricEntityType.Builder.class.getDeclaredMethod("build").getReturnType())
                .isEqualTo(EntityType.class);
        assertThat(FabricEntityType.Builder.class.getDeclaredMethod("createLiving",
                EntityType.EntityFactory.class, MobCategory.class,
                java.util.function.UnaryOperator.class).getReturnType())
                .isEqualTo(EntityType.Builder.class);
        assertThat(FabricEntityType.Builder.class.getDeclaredMethod("createMob",
                EntityType.EntityFactory.class, MobCategory.class,
                java.util.function.UnaryOperator.class).getReturnType())
                .isEqualTo(EntityType.Builder.class);
        assertThat(FabricEntityType.Builder.Living.class.getDeclaredMethod(
                "defaultAttributes", java.util.function.Supplier.class).getReturnType())
                .isEqualTo(FabricEntityType.Builder.Living.class);
        assertThat(FabricEntityType.Builder.Mob.class.getDeclaredMethod(
                "spawnRestriction", net.minecraft.world.entity.SpawnPlacementType.class,
                net.minecraft.world.level.levelgen.Heightmap.Types.class,
                net.minecraft.world.entity.SpawnPlacements.SpawnPredicate.class).getReturnType())
                .isEqualTo(FabricEntityType.Builder.Mob.class);
    }
}
