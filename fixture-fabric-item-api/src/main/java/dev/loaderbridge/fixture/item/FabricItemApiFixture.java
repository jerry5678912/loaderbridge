package dev.loaderbridge.fixture.item;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.FabricComponentMapBuilder;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.fabricmc.fabric.api.item.v1.FabricTooltipType;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

public final class FabricItemApiFixture implements ModInitializer {
    private static Item fabricTool;

    @Override
    public void onInitialize() {
        Item.Properties properties = new Item.Properties().durability(10);
        FabricItem.Settings fabricProperties = (FabricItem.Settings) (Object) properties;
        fabricProperties.equipmentSlot((entity, stack) -> EquipmentSlot.HEAD);
        fabricProperties.customDamage((stack, amount, entity, slot, breakCallback) -> 2);
        fabricTool = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(
                        "loaderbridge_item_api_fixture", "fabric_tool"),
                new FabricTool(properties));
        DefaultItemComponentEvents.MODIFY.register(context -> context.modify(
                fabricTool, builder -> builder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

        ItemStack stack = new ItemStack(fabricTool);
        DataComponentMap.Builder componentBuilder = DataComponentMap.builder();
        int maximum = ((FabricComponentMapBuilder) componentBuilder)
                .getOrDefault(DataComponents.MAX_STACK_SIZE, 17);
        if (!(Items.STONE instanceof FabricItem)
                || !((Object) stack instanceof FabricItemStack)
                || !((Object) TooltipFlag.NORMAL instanceof FabricTooltipType)
                || maximum != 17
                || componentBuilder.build().get(DataComponents.MAX_STACK_SIZE) != 17
                || !((FabricItemStack) (Object) stack).getRecipeRemainder().is(Items.GOLD_NUGGET)
                || !((FabricItemStack) (Object) stack).getCreatorNamespace()
                        .equals("loaderbridge_item_api_fixture")) {
            throw new IllegalStateException("Fabric item common contracts failed");
        }
        System.out.println("LOADERBRIDGE_FABRIC_ITEM_CONTENT_READY");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ArmorStand entity = EntityType.ARMOR_STAND.create(server.overworld());
            if (entity == null) throw new IllegalStateException("could not create item fixture entity");
            if (!new ItemStack(fabricTool).getOrDefault(
                    DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false)) {
                throw new IllegalStateException("Fabric default item component event failed");
            }
            ItemStack damaged = new ItemStack(fabricTool);
            damaged.hurtAndBreak(5, entity, EquipmentSlot.HEAD);
            if (entity.getEquipmentSlotForItem(damaged) != EquipmentSlot.HEAD
                    || damaged.getDamageValue() != 2) {
                throw new IllegalStateException("Fabric item behavior hooks failed");
            }
            System.out.println("LOADERBRIDGE_FABRIC_ITEM_API_READY damage=2,slot=head,glint=true");
        });
    }

    private static final class FabricTool extends Item implements FabricItem {
        private FabricTool(Properties properties) { super(properties); }
        @Override public ItemStack getRecipeRemainder(ItemStack stack) {
            return new ItemStack(Items.GOLD_NUGGET);
        }
        @Override public String getCreatorNamespace(ItemStack stack) {
            return "loaderbridge_item_api_fixture";
        }
    }
}
