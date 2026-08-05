package dev.loaderbridge.fixture.item;

import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentSource;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.FabricComponentMapBuilder;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.fabricmc.fabric.api.item.v1.FabricTooltipType;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.AddValue;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

public final class FabricItemApiFixture implements ModInitializer {
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment>
            CREATOR_ENCHANTMENT = ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(
                            "loaderbridge_item_api_fixture", "creator_enchantment"));
    private static Item fabricTool;
    private static Holder<Potion> fixturePotion;
    private static int remainderCalls;
    private static int primaryEnchantingCalls;
    private static int acceptableEnchantingCalls;
    private static int modifyEnchantingCalls;
    private static EnchantmentSource sharpnessSource;
    private static EnchantmentSource creatorEnchantmentSource;

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
        fixturePotion = Registry.registerForHolder(BuiltInRegistries.POTION,
                ResourceLocation.fromNamespaceAndPath(
                        "loaderbridge_item_api_fixture", "creator_potion"),
                new Potion());
        FuelRegistry.INSTANCE.add(fabricTool, 200);
        FabricBrewingRecipeRegistryBuilder.BUILD.register(builder ->
                ((FabricBrewingRecipeRegistryBuilder) (Object) builder)
                        .registerPotionRecipe(Potions.WATER,
                                Ingredient.of(fabricTool), Potions.AWKWARD));
        DefaultItemComponentEvents.MODIFY.register(context -> context.modify(
                fabricTool, builder -> builder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) -> {
            if (!target.is(fabricTool) || !enchantment.is(Enchantments.SHARPNESS)) {
                return net.fabricmc.fabric.api.util.TriState.DEFAULT;
            }
            if (context == EnchantingContext.PRIMARY) primaryEnchantingCalls++;
            if (context == EnchantingContext.ACCEPTABLE) acceptableEnchantingCalls++;
            return net.fabricmc.fabric.api.util.TriState.TRUE;
        });
        EnchantmentEvents.MODIFY.register((key, builder, source) -> {
            if (key.equals(CREATOR_ENCHANTMENT)) {
                creatorEnchantmentSource = source;
                return;
            }
            if (!key.equals(Enchantments.SHARPNESS)) return;
            sharpnessSource = source;
            modifyEnchantingCalls++;
            builder.withEffect(EnchantmentEffectComponents.REPAIR_WITH_XP,
                    new AddValue(LevelBasedValue.constant(3.0F)));
        });

        ItemStack stack = new ItemStack(fabricTool);
        ItemStack potionStack = PotionContents.createItemStack(Items.POTION, fixturePotion);
        ItemStack arrowStack = PotionContents.createItemStack(Items.TIPPED_ARROW, fixturePotion);
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
                        .equals("loaderbridge_item_api_fixture")
                || !((FabricItemStack) (Object) potionStack).getCreatorNamespace()
                        .equals("loaderbridge_item_api_fixture")
                || !((FabricItemStack) (Object) arrowStack).getCreatorNamespace()
                        .equals("loaderbridge_item_api_fixture")) {
            throw new IllegalStateException("Fabric item common contracts failed");
        }
        System.out.println("LOADERBRIDGE_FABRIC_ITEM_CONTENT_READY");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            int primaryCallsBefore = primaryEnchantingCalls;
            int acceptableCallsBefore = acceptableEnchantingCalls;
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
            int callsBeforeRecipe = remainderCalls;
            NonNullList<ItemStack> remainders = remainderTestRecipe().getRemainingItems(
                    CraftingInput.of(1, 3, List.of(
                            new ItemStack(fabricTool),
                            new ItemStack(Items.WATER_BUCKET),
                            new ItemStack(Items.DIAMOND))));
            int callsAfterRecipe = remainderCalls;
            if (!remainders.get(0).is(Items.GOLD_NUGGET)
                    || !remainders.get(1).is(Items.BUCKET)
                    || !remainders.get(2).isEmpty()
                    || callsAfterRecipe != callsBeforeRecipe + 1) {
                throw new IllegalStateException("Fabric recipe remainder pipeline failed");
            }
            BlockPos furnacePos = server.overworld().getSharedSpawnPos().offset(4, 0, 0);
            server.overworld().setBlock(furnacePos, Blocks.FURNACE.defaultBlockState(), 3);
            FurnaceBlockEntity furnace = (FurnaceBlockEntity)
                    server.overworld().getBlockEntity(furnacePos);
            furnace.setItem(0, new ItemStack(Blocks.COBBLESTONE));
            furnace.setItem(1, new ItemStack(fabricTool));
            for (int tick = 0; tick < 200; tick++) {
                AbstractFurnaceBlockEntity.serverTick(server.overworld(), furnacePos,
                        server.overworld().getBlockState(furnacePos), furnace);
            }
            if (!furnace.getItem(0).isEmpty()
                    || !furnace.getItem(1).is(Items.GOLD_NUGGET)
                    || !furnace.getItem(2).is(Items.STONE)
                    || remainderCalls != callsAfterRecipe + 1) {
                throw new IllegalStateException("Fabric furnace remainder pipeline failed");
            }
            int callsAfterFurnace = remainderCalls;
            BlockPos brewingPos = server.overworld().getSharedSpawnPos().offset(6, 0, 0);
            server.overworld().setBlock(brewingPos,
                    Blocks.BREWING_STAND.defaultBlockState(), 3);
            BrewingStandBlockEntity brewingStand = (BrewingStandBlockEntity)
                    server.overworld().getBlockEntity(brewingPos);
            for (int slot = 0; slot < 3; slot++) {
                brewingStand.setItem(slot,
                        PotionContents.createItemStack(Items.POTION, Potions.WATER));
            }
            brewingStand.setItem(3, new ItemStack(fabricTool));
            brewingStand.setItem(4, new ItemStack(Items.BLAZE_POWDER));
            for (int tick = 0; tick < 420; tick++) {
                BrewingStandBlockEntity.serverTick(server.overworld(), brewingPos,
                        server.overworld().getBlockState(brewingPos), brewingStand);
            }
            for (int slot = 0; slot < 3; slot++) {
                if (!brewingStand.getItem(slot).getOrDefault(
                        DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                        .is(Potions.AWKWARD)) {
                    throw new IllegalStateException("Fabric brewing output pipeline failed");
                }
            }
            if (!brewingStand.getItem(3).is(Items.GOLD_NUGGET)
                    || remainderCalls != callsAfterFurnace + 1) {
                throw new IllegalStateException("Fabric brewing remainder pipeline failed");
            }
            Registry<net.minecraft.world.item.enchantment.Enchantment> enchantments =
                    server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            var sharpness = enchantments.getHolderOrThrow(Enchantments.SHARPNESS);
            var creatorEnchantment = enchantments.getHolderOrThrow(CREATOR_ENCHANTMENT);
            ItemStack creatorBook = EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(creatorEnchantment, 1));
            if (!((FabricItemStack) (Object) creatorBook).getCreatorNamespace()
                    .equals("loaderbridge_item_api_fixture")
                    || creatorEnchantmentSource != EnchantmentSource.MOD) {
                throw new IllegalStateException("Fabric enchanted-book creator failed: "
                        + creatorEnchantmentSource);
            }
            ItemStack enchantingTarget = new ItemStack(fabricTool);
            var availableEnchantments = EnchantmentHelper.getAvailableEnchantmentResults(
                    20, enchantingTarget, java.util.stream.Stream.of(sharpness));
            if (availableEnchantments.isEmpty()
                    || primaryEnchantingCalls != primaryCallsBefore + 1) {
                throw new IllegalStateException("Fabric primary enchanting pipeline failed: "
                        + "available=" + availableEnchantments.size()
                        + ",before=" + primaryCallsBefore
                        + ",after=" + primaryEnchantingCalls);
            }
            String targetTag = "loaderbridge_item_enchanting_"
                    + entity.getUUID().toString().replace("-", "");
            entity.addTag(targetTag);
            entity.setItemSlot(EquipmentSlot.MAINHAND, enchantingTarget);
            BlockPos spawn = server.overworld().getSharedSpawnPos();
            entity.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    0.0F, 0.0F);
            if (!server.overworld().addFreshEntity(entity)) {
                throw new IllegalStateException("could not add item fixture entity");
            }
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withPermission(4),
                    "enchant @e[type=minecraft:armor_stand,"
                            + "tag=" + targetTag + ",limit=1] "
                            + "minecraft:sharpness 1");
            int repairedXp = EnchantmentHelper.modifyDurabilityToRepairFromXp(
                    server.overworld(), entity.getMainHandItem(), 1);
            if (EnchantmentHelper.getItemEnchantmentLevel(
                            sharpness, entity.getMainHandItem()) != 1
                    || acceptableEnchantingCalls != acceptableCallsBefore + 1
                    || modifyEnchantingCalls < 1
                    || sharpnessSource != EnchantmentSource.VANILLA
                    || repairedXp != 4) {
                throw new IllegalStateException("Fabric enchanting events pipeline failed: "
                        + "primary=" + primaryEnchantingCalls
                        + ",acceptable=" + acceptableEnchantingCalls
                        + ",modify=" + modifyEnchantingCalls
                        + ",source=" + sharpnessSource + ",repair=" + repairedXp);
            }
            System.out.println("LOADERBRIDGE_FABRIC_ITEM_API_READY "
                    + "damage=2,slot=head,glint=true,remainder=gold_nugget,"
                    + "furnace=stone,brewing=awkward,enchanting=sharpness,"
                    + "creator=potion+book");
        });
    }

    private static Recipe<CraftingInput> remainderTestRecipe() {
        return new Recipe<>() {
            @Override public boolean matches(CraftingInput input, Level level) { return true; }
            @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
                return ItemStack.EMPTY;
            }
            @Override public boolean canCraftInDimensions(int width, int height) { return true; }
            @Override public ItemStack getResultItem(HolderLookup.Provider lookup) {
                return ItemStack.EMPTY;
            }
            @Override public RecipeSerializer<?> getSerializer() { return null; }
            @Override public RecipeType<?> getType() { return RecipeType.CRAFTING; }
        };
    }

    private static final class FabricTool extends Item implements FabricItem {
        private FabricTool(Properties properties) { super(properties); }
        @Override public ItemStack getRecipeRemainder(ItemStack stack) {
            remainderCalls++;
            return new ItemStack(Items.GOLD_NUGGET);
        }
        @Override public String getCreatorNamespace(ItemStack stack) {
            return "loaderbridge_item_api_fixture";
        }
    }
}
