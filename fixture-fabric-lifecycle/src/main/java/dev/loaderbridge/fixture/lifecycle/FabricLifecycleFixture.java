package dev.loaderbridge.fixture.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FlattenableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import com.mojang.serialization.Lifecycle;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.network.chat.Component;

/** Verifies Forge-to-Fabric server and world tick ordering at runtime. */
public final class FabricLifecycleFixture implements ModInitializer {
    private static final GameRules.Key<GameRules.BooleanValue> PERSISTED_RULE =
            GameRuleRegistry.register("loaderbridgeEnabled", GameRules.Category.MISC,
                    GameRuleFactory.createBooleanRule(false, (server, rule) -> {
                        if (rule.get()) System.out.println("LOADERBRIDGE_FABRIC_GAME_RULE_CHANGED");
                    }));
    private static final AtomicInteger STATE = new AtomicInteger();
    private static final AtomicInteger SERVER_STATE = new AtomicInteger();
    private static final AtomicInteger WORLDS_LOADED = new AtomicInteger();
    private static final AtomicInteger WORLDS_UNLOADED = new AtomicInteger();
    private static final AtomicBoolean REPORTED = new AtomicBoolean();
    private static final AtomicBoolean CHUNK_LOADED = new AtomicBoolean();
    private static final AtomicBoolean CHUNK_GENERATED = new AtomicBoolean();
    private static final AtomicBoolean CHUNK_FULL = new AtomicBoolean();
    private static final AtomicBoolean CHUNK_UNLOADED = new AtomicBoolean();
    private static final AtomicBoolean TRACKING_LOOKUP_REPORTED = new AtomicBoolean();
    private static final AtomicBoolean TRACKING_ENTITY_SPAWNED = new AtomicBoolean();
    private static final AtomicInteger RESOURCE_RELOADS = new AtomicInteger();
    private static final AtomicBoolean CUSTOM_REGISTRY_CALLBACK = new AtomicBoolean();
    private static final AtomicBoolean DYNAMIC_REGISTRY_SETUP = new AtomicBoolean();
    private static final AtomicBoolean DYNAMIC_REGISTRY_CALLBACK = new AtomicBoolean();
    private static final int TEST_CHUNK = 725;
    private static EntityType<ArmorStand> attributeFixtureType;
    private static EntityType<Zombie> mobBuilderFixtureType;
    private static final BlockApiLookup<String, Void> BLOCK_LOOKUP = BlockApiLookup.get(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_block_lookup"),
            String.class, Void.class);
    private static final ItemApiLookup<String, Void> ITEM_LOOKUP = ItemApiLookup.get(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_item_lookup"),
            String.class, Void.class);
    private static final ItemApiLookup<Item, Void> ITEM_SELF_LOOKUP = ItemApiLookup.get(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_item_self_lookup"),
            Item.class, Void.class);
    private static final EntityApiLookup<String, Void> ENTITY_LOOKUP = EntityApiLookup.get(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_entity_lookup"),
            String.class, Void.class);
    private static final EntityApiLookup<Entity, Void> ENTITY_SELF_LOOKUP = EntityApiLookup.get(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_entity_self_lookup"),
            Entity.class, Void.class);
    private static final ResourceKey<Registry<String>> CUSTOM_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(
                    "loaderbridge", "fixture_registry"));
    private static final MappedRegistry<String> CUSTOM_REGISTRY =
            new MappedRegistry<>(CUSTOM_REGISTRY_KEY, Lifecycle.stable(), false);
    private static final ResourceKey<Registry<String>> DYNAMIC_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(
                    "loaderbridge", "fixture_dynamic"));
    private static final ResourceKey<Registry<String>> EMPTY_DYNAMIC_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(
                    "loaderbridge", "fixture_empty_dynamic"));
    static final ResourceKey<CreativeModeTab> ITEM_GROUP_KEY = ResourceKey.create(
            net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_items"));

    @Override
    @SuppressWarnings("deprecation")
    public void onInitialize() {
        TransactionFixture transactionFixture = new TransactionFixture();
        try (Transaction outer = Transaction.openOuter()) {
            transactionFixture.set(1, outer);
            try (Transaction nested = outer.openNested()) {
                transactionFixture.set(2, nested);
                nested.commit();
            }
        }
        try (Transaction committed = Transaction.openOuter()) {
            transactionFixture.set(3, committed);
            committed.commit();
        }
        if (transactionFixture.value != 3 || transactionFixture.commits != 1
                || Transaction.isOpen()) {
            throw new IllegalStateException("LOADERBRIDGE_FABRIC_TRANSFER_TRANSACTION_FAILED");
        }
        System.out.println("LOADERBRIDGE_FABRIC_TRANSFER_TRANSACTION_READY");
        TransactionalStorage storage = new TransactionalStorage();
        try (Transaction aborted = Transaction.openOuter()) {
            if (storage.insert("energy", 80, aborted) != 80) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_TRANSFER_INSERT_FAILED");
            }
        }
        try (Transaction committed = Transaction.openOuter()) {
            storage.insert("energy", 70, committed);
            committed.commit();
        }
        try (Transaction aborted = Transaction.openOuter()) {
            storage.extract("energy", 20, aborted);
        }
        try (Transaction committed = Transaction.openOuter()) {
            storage.extract("energy", 30, committed);
            committed.commit();
        }
        if (storage.getAmount() != 40
                || !storage.nonEmptyIterator().hasNext()
                || Storage.<String>empty().supportsInsertion()) {
            throw new IllegalStateException("LOADERBRIDGE_FABRIC_TRANSFER_STORAGE_FAILED");
        }
        System.out.println("LOADERBRIDGE_FABRIC_TRANSFER_STORAGE_READY");
        TransactionalStorage secondStorage = new TransactionalStorage();
        CombinedSlottedStorage<String, TransactionalStorage> combinedStorage =
                new CombinedSlottedStorage<>(java.util.List.of(storage, secondStorage));
        try (Transaction committed = Transaction.openOuter()) {
            if (combinedStorage.insert("energy", 120, committed) != 120) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_TRANSFER_COMBINED_FAILED");
            }
            committed.commit();
        }
        Storage<String> readOnly = FilteringStorage.readOnlyOf(combinedStorage);
        try (Transaction aborted = Transaction.openOuter()) {
            if (readOnly.insert("energy", 1, aborted) != 0
                    || readOnly.iterator().next().extract("energy", 1, aborted) != 0) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_TRANSFER_FILTER_FAILED");
            }
        }
        if (combinedStorage.getSlotCount() != 2
                || combinedStorage.getSlot(1) != secondStorage
                || storage.getAmount() != 100
                || secondStorage.getAmount() != 60) {
            throw new IllegalStateException("LOADERBRIDGE_FABRIC_TRANSFER_SLOTTED_FAILED");
        }
        System.out.println("LOADERBRIDGE_FABRIC_TRANSFER_COMPOSITION_READY");
        TransactionalStorage utilityTarget = new TransactionalStorage();
        if (StorageUtil.move(secondStorage, utilityTarget, "energy"::equals, 25, null) != 25
                || secondStorage.getAmount() != 35
                || utilityTarget.getAmount() != 25
                || !"energy".equals(StorageUtil.findExtractableResource(utilityTarget, null))) {
            throw new IllegalStateException("LOADERBRIDGE_FABRIC_TRANSFER_UTILITIES_FAILED");
        }
        System.out.println("LOADERBRIDGE_FABRIC_TRANSFER_UTILITIES_READY");
        SimpleContainer itemInventory = new SimpleContainer(2);
        InventoryStorage itemStorage = InventoryStorage.of(itemInventory, null);
        ItemVariant diamonds = ItemVariant.of(Items.DIAMOND);
        try (Transaction aborted = Transaction.openOuter()) {
            if (itemStorage.insert(diamonds, 70, aborted) != 70) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_ITEM_ABORT_SETUP_FAILED");
            }
        }
        try (Transaction committed = Transaction.openOuter()) {
            if (itemStorage.insert(diamonds, 70, committed) != 70) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_ITEM_INSERT_FAILED");
            }
            committed.commit();
        }
        try (Transaction committed = Transaction.openOuter()) {
            if (itemStorage.getSlot(0).extract(diamonds, 9, committed) != 9) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_ITEM_EXTRACT_FAILED");
            }
            committed.commit();
        }
        if (itemStorage.getSlotCount() != 2
                || itemInventory.getItem(0).getCount() != 55
                || itemInventory.getItem(1).getCount() != 6
                || !diamonds.matches(itemInventory.getItem(0))) {
            throw new IllegalStateException("LOADERBRIDGE_FABRIC_ITEM_STORAGE_FAILED");
        }
        System.out.println("LOADERBRIDGE_FABRIC_ITEM_STORAGE_READY");
        ContainerItemContext constantContext =
                ContainerItemContext.withConstant(new ItemStack(Items.DIAMOND, 3));
        try (Transaction committed = Transaction.openOuter()) {
            if (constantContext.exchange(ItemVariant.of(Items.DIRT), 2, committed) != 2) {
                throw new IllegalStateException(
                        "LOADERBRIDGE_FABRIC_CONSTANT_ITEM_CONTEXT_EXCHANGE_FAILED");
            }
            committed.commit();
        }
        if (constantContext.getAmount() != 3
                || !constantContext.getItemVariant().equals(diamonds)) {
            throw new IllegalStateException(
                    "LOADERBRIDGE_FABRIC_CONSTANT_ITEM_CONTEXT_FAILED");
        }
        System.out.println("LOADERBRIDGE_FABRIC_CONSTANT_ITEM_CONTEXT_READY");
        Block flattenInput = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "flatten_input"),
                new Block(BlockBehaviour.Properties.of()));
        Block flattenOutput = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "flatten_output"),
                new Block(BlockBehaviour.Properties.of()));
        Block stripInput = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "strip_input"),
                new RotatedPillarBlock(BlockBehaviour.Properties.of()));
        Block stripOutput = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "strip_output"),
                new RotatedPillarBlock(BlockBehaviour.Properties.of()));
        Block oxidized = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "oxidized"),
                new Block(BlockBehaviour.Properties.of()));
        Block waxed = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "waxed"),
                new Block(BlockBehaviour.Properties.of()));
        FuelRegistry.INSTANCE.add(Items.DIAMOND, 1234);
        CompostingChanceRegistry.INSTANCE.add(Items.DIAMOND, 0.42F);
        FlammableBlockRegistry.getDefaultInstance().add(flattenInput, 7, 11);
        FlattenableBlockRegistry.register(flattenInput, flattenOutput.defaultBlockState());
        StrippableBlockRegistry.register(stripInput, stripOutput);
        OxidizableBlocksRegistry.registerOxidizableBlockPair(flattenInput, oxidized);
        OxidizableBlocksRegistry.registerWaxableBlockPair(oxidized, waxed);
        FlammableBlockRegistry.Entry flammable =
                FlammableBlockRegistry.getDefaultInstance().get(flattenInput);
        if (FuelRegistry.INSTANCE.get(Items.DIAMOND) != 1234
                || Math.abs(CompostingChanceRegistry.INSTANCE.get(Items.DIAMOND) - 0.42F) > 0.001F
                || flammable.getBurnChance() != 7
                || flammable.getSpreadChance() != 11
                || !ShovelItem.getShovelPathingState(flattenInput.defaultBlockState())
                        .is(flattenOutput)
                || !AxeItem.getAxeStrippingState(stripInput.defaultBlockState()).is(stripOutput)
                || WeatheringCopper.getNext(flattenInput).orElseThrow() != oxidized
                || WeatheringCopper.getPrevious(oxidized).orElseThrow() != flattenInput
                || HoneycombItem.getWaxed(oxidized.defaultBlockState()).orElseThrow().getBlock()
                        != waxed) {
            throw new IllegalStateException("LOADERBRIDGE_FABRIC_CONTENT_REGISTRIES_FAILED");
        }
        System.out.println("LOADERBRIDGE_FABRIC_CONTENT_REGISTRIES_READY");
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ITEM_GROUP_KEY.location(),
                FabricItemGroup.builder()
                        .title(Component.literal("LoaderBridge Fixture"))
                        .icon(() -> new ItemStack(Items.DIAMOND))
                        .displayItems((parameters, output) -> {})
                        .build());
        ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP_KEY).register(entries -> {
            entries.accept(Items.DIAMOND);
            if (entries.getDisplayStacks().stream().noneMatch(stack -> stack.is(Items.DIAMOND))
                    || entries.getSearchTabStacks().stream()
                            .noneMatch(stack -> stack.is(Items.DIAMOND))) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_ITEM_GROUP_FAILED");
            }
            System.out.println("LOADERBRIDGE_FABRIC_ITEM_GROUP_READY");
        });
        DynamicRegistrySetupCallback.EVENT.register(view -> {
            if (view.getOptional(DYNAMIC_REGISTRY_KEY).isEmpty()) return;
            if (view.asDynamicRegistryManager().registry(DYNAMIC_REGISTRY_KEY).isEmpty()
                    || view.stream().noneMatch(registry -> registry.key() == DYNAMIC_REGISTRY_KEY)) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_DYNAMIC_VIEW_FAILED");
            }
            DYNAMIC_REGISTRY_SETUP.set(true);
            view.registerEntryAdded(DYNAMIC_REGISTRY_KEY, (rawId, id, value) -> {
                if (id.equals(ResourceLocation.fromNamespaceAndPath("loaderbridge", "value"))
                        && value.equals("dynamic-value")) {
                    DYNAMIC_REGISTRY_CALLBACK.set(true);
                }
            });
        });
        DynamicRegistries.registerSynced(DYNAMIC_REGISTRY_KEY,
                com.mojang.serialization.Codec.STRING);
        DynamicRegistries.registerSynced(EMPTY_DYNAMIC_REGISTRY_KEY,
                com.mojang.serialization.Codec.STRING,
                DynamicRegistries.SyncOption.SKIP_WHEN_EMPTY);
        RegistryEntryAddedCallback.event(CUSTOM_REGISTRY).register((rawId, id, value) -> {
            if (rawId == 0
                    && id.equals(ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_value"))
                    && value.equals("registry-value")) {
                CUSTOM_REGISTRY_CALLBACK.set(true);
            }
        });
        FabricRegistryBuilder.from(CUSTOM_REGISTRY)
                .attribute(RegistryAttribute.SYNCED)
                .buildAndRegister();
        Registry.register(CUSTOM_REGISTRY,
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_value"),
                "registry-value");
        if (!CUSTOM_REGISTRY_CALLBACK.get()
                || !RegistryAttributeHolder.get(CUSTOM_REGISTRY).hasAttribute(RegistryAttribute.MODDED)
                || !RegistryAttributeHolder.get(CUSTOM_REGISTRY).hasAttribute(RegistryAttribute.SYNCED)
                || BuiltInRegistries.REGISTRY.get(CUSTOM_REGISTRY_KEY.location()) != CUSTOM_REGISTRY) {
            throw new IllegalStateException("LOADERBRIDGE_FABRIC_REGISTRY_SYNC_FAILED");
        }
        System.out.println("LOADERBRIDGE_FABRIC_REGISTRY_SYNC_READY");
        BLOCK_LOOKUP.registerForBlocks(
                (world, pos, state, blockEntity, context) -> "direct", Blocks.STONE);
        BLOCK_LOOKUP.registerFallback((world, pos, state, blockEntity, context) ->
                state.is(Blocks.DIRT) ? "fallback" : null);
        ITEM_LOOKUP.registerForItems((stack, context) -> "direct", Items.DIAMOND);
        ITEM_LOOKUP.registerFallback((stack, context) ->
                stack.is(Items.DIRT) ? "fallback" : null);
        ITEM_SELF_LOOKUP.registerSelf(Items.DIAMOND);
        ENTITY_LOOKUP.registerForType((entity, context) -> "direct", EntityType.ARMOR_STAND);
        ENTITY_LOOKUP.registerFallback((entity, context) ->
                entity.getType() == EntityType.ZOMBIE ? "fallback" : null);
        ENTITY_SELF_LOOKUP.registerSelf(EntityType.ARMOR_STAND);
        var bridgedBlockEntityType = FabricBlockEntityTypeBuilder
                .create((position, state) -> null, Blocks.STONE)
                .addBlock(Blocks.DIRT)
                .addBlocks(Blocks.GRANITE, Blocks.DIORITE)
                .build();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_block_entity"),
                bridgedBlockEntityType);
        attributeFixtureType = FabricEntityTypeBuilder.<ArmorStand>createLiving()
                .spawnGroup(MobCategory.MISC)
                .entityFactory(ArmorStand::new)
                .dimensions(EntityDimensions.fixed(0.5F, 1.975F))
                .trackRangeBlocks(80)
                .trackedUpdateRate(3)
                .forceTrackedVelocityUpdates(true)
                .defaultAttributes(ArmorStand::createAttributes)
                .build();
        Registry.register(BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_attribute_entity"),
                attributeFixtureType);
        EntityType.Builder<Zombie> modernMobBuilder = FabricEntityType.Builder.createMob(
                Zombie::new, MobCategory.MONSTER,
                builder -> builder.defaultAttributes(Zombie::createAttributes)
                        .spawnRestriction(SpawnPlacementTypes.ON_GROUND,
                                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                Mob::checkMobSpawnRules));
        modernMobBuilder.sized(0.6F, 1.95F);
        @SuppressWarnings("unchecked")
        FabricEntityType.Builder<Zombie> fabricModernMobBuilder =
                (FabricEntityType.Builder<Zombie>) (Object) modernMobBuilder;
        fabricModernMobBuilder.alwaysUpdateVelocity(true);
        mobBuilderFixtureType = fabricModernMobBuilder.build();
        Registry.register(BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "fixture_mob_builder_entity"),
                mobBuilderFixtureType);
        if (!bridgedBlockEntityType.isValid(Blocks.STONE.defaultBlockState())
                || !bridgedBlockEntityType.isValid(Blocks.DIRT.defaultBlockState())
                || !bridgedBlockEntityType.isValid(Blocks.GRANITE.defaultBlockState())
                || !bridgedBlockEntityType.isValid(Blocks.DIORITE.defaultBlockState())
                || bridgedBlockEntityType.isValid(Blocks.OAK_PLANKS.defaultBlockState())) {
            throw new IllegalStateException("LOADERBRIDGE_FABRIC_OBJECT_BUILDER_FAILED");
        }
        System.out.println("LOADERBRIDGE_FABRIC_OBJECT_BUILDER_READY");
        if (SpawnPlacements.getPlacementType(mobBuilderFixtureType) != SpawnPlacementTypes.ON_GROUND
                || SpawnPlacements.getHeightmapType(mobBuilderFixtureType)
                        != Heightmap.Types.MOTION_BLOCKING_NO_LEAVES) {
            throw new IllegalStateException("LOADERBRIDGE_FABRIC_ENTITY_BUILDER_FAILED");
        }
        System.out.println("LOADERBRIDGE_FABRIC_ENTITY_BUILDER_READY");
        System.out.println("LOADERBRIDGE_FABRIC_MODERN_ENTITY_BUILDER_READY");
        PayloadTypeRegistry.playC2S().register(FabricNetworkingPayload.PONG_TYPE,
                FabricNetworkingPayload.PONG_CODEC);
        PayloadTypeRegistry.playS2C().register(FabricNetworkingPayload.PING_TYPE,
                FabricNetworkingPayload.PING_CODEC);
        PayloadTypeRegistry.configurationC2S().register(FabricNetworkingPayload.CONFIG_PONG_TYPE,
                FabricNetworkingPayload.CONFIG_PONG_CODEC);
        PayloadTypeRegistry.configurationS2C().register(FabricNetworkingPayload.CONFIG_PING_TYPE,
                FabricNetworkingPayload.CONFIG_PING_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FabricNetworkingPayload.PONG_TYPE,
                (payload, context) -> {
                    if (payload.value().equals("pong")) {
                        System.out.println("LOADERBRIDGE_FABRIC_NETWORK_SERVER_ROUNDTRIP");
                    }
                });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            boolean lookupReady = PlayerLookup.all(server).contains(player)
                    && PlayerLookup.world(player.serverLevel()).contains(player)
                    && PlayerLookup.tracking(player.serverLevel(), player.chunkPosition()).contains(player);
            if (!lookupReady) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_PLAYER_LOOKUP_FAILED");
            }
            System.out.println("LOADERBRIDGE_FABRIC_PLAYER_LOOKUP_READY");
            int previousSelected = player.getInventory().selected;
            ItemStack previousFirstSlot = player.getInventory().getItem(0).copy();
            ItemStack previousSecondSlot = player.getInventory().getItem(1).copy();
            ItemStack previousCarried = player.containerMenu.getCarried().copy();
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, ItemStack.EMPTY);
            player.getInventory().setItem(1, ItemStack.EMPTY);
            PlayerInventoryStorage playerStorage = PlayerInventoryStorage.of(player);
            ItemVariant playerDiamonds = ItemVariant.of(Items.DIAMOND);
            try (Transaction committed = Transaction.openOuter()) {
                if (playerStorage.offer(playerDiamonds, 5, committed) != 5) {
                    throw new IllegalStateException(
                            "LOADERBRIDGE_FABRIC_PLAYER_ITEM_OFFER_FAILED");
                }
                committed.commit();
            }
            ContainerItemContext handContext =
                    ContainerItemContext.ofPlayerHand(player, InteractionHand.MAIN_HAND);
            try (Transaction committed = Transaction.openOuter()) {
                if (handContext.exchange(ItemVariant.of(Items.DIRT), 2, committed) != 2) {
                    throw new IllegalStateException(
                            "LOADERBRIDGE_FABRIC_PLAYER_ITEM_EXCHANGE_FAILED");
                }
                committed.commit();
            }
            var cursor = PlayerInventoryStorage.getCursorStorage(player.containerMenu);
            player.containerMenu.setCarried(ItemStack.EMPTY);
            try (Transaction committed = Transaction.openOuter()) {
                if (cursor.insert(playerDiamonds, 1, committed) != 1) {
                    throw new IllegalStateException(
                            "LOADERBRIDGE_FABRIC_PLAYER_CURSOR_INSERT_FAILED");
                }
                committed.commit();
            }
            if (player.getInventory().getItem(0).getCount() != 3
                    || !player.getInventory().getItem(0).is(Items.DIAMOND)
                    || player.getInventory().getItem(1).getCount() != 2
                    || !player.getInventory().getItem(1).is(Items.DIRT)
                    || cursor.getAmount() != 1
                    || !cursor.getResource().equals(playerDiamonds)) {
                throw new IllegalStateException(
                        "LOADERBRIDGE_FABRIC_PLAYER_ITEM_CONTEXT_FAILED");
            }
            player.getInventory().setItem(0, previousFirstSlot);
            player.getInventory().setItem(1, previousSecondSlot);
            player.getInventory().selected = previousSelected;
            player.containerMenu.setCarried(previousCarried);
            player.containerMenu.broadcastChanges();
            System.out.println("LOADERBRIDGE_FABRIC_PLAYER_ITEM_CONTEXT_READY");
            sender.sendPacket(new FabricNetworkingPayload(FabricNetworkingPayload.PING_TYPE, "ping"));
            if (TRACKING_ENTITY_SPAWNED.compareAndSet(false, true)) {
                var entity = EntityType.ARMOR_STAND.create(player.serverLevel());
                if (entity == null) {
                    throw new IllegalStateException("LOADERBRIDGE_FABRIC_TRACKING_ENTITY_CREATE_FAILED");
                }
                entity.setPos(player.getX() + 2.0, player.getY(), player.getZ());
                entity.addTag("loaderbridge_entity_fixture");
                if (!player.serverLevel().addFreshEntity(entity)) {
                    throw new IllegalStateException("LOADERBRIDGE_FABRIC_TRACKING_ENTITY_SPAWN_FAILED");
                }
            }
        });
        ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register((handler, server) ->
                System.out.println("LOADERBRIDGE_FABRIC_SERVER_BEFORE_CONFIGURE"));
        ServerConfigurationNetworking.registerGlobalReceiver(
                FabricNetworkingPayload.CONFIG_PONG_TYPE, (payload, context) -> {
                    if (payload.value().equals("config_pong")) {
                        System.out.println("LOADERBRIDGE_FABRIC_CONFIG_SERVER_ROUNDTRIP");
                    }
                });
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            System.out.println("LOADERBRIDGE_FABRIC_SERVER_CONFIGURE");
            if (!ServerConfigurationNetworking.canSend(
                    handler, FabricNetworkingPayload.CONFIG_PING_TYPE)) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_CONFIG_SERVER_CANNOT_SEND");
            }
            ServerConfigurationNetworking.send(handler, new FabricNetworkingPayload(
                    FabricNetworkingPayload.CONFIG_PING_TYPE, "config_ping"));
        });
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity.getTags().contains("loaderbridge_entity_fixture")) {
                System.out.println("LOADERBRIDGE_FABRIC_TRACKING_STARTED");
                player.server.execute(() -> {
                    if (PlayerLookup.tracking(entity).contains(player)
                            && TRACKING_LOOKUP_REPORTED.compareAndSet(false, true)) {
                        System.out.println("LOADERBRIDGE_FABRIC_ENTITY_LOOKUP_READY");
                    }
                });
            }
        });
        EntityTrackingEvents.STOP_TRACKING.register((entity, player) -> {
            if (entity.getTags().contains("loaderbridge_entity_fixture")) {
                System.out.println("LOADERBRIDGE_FABRIC_TRACKING_STOPPED");
            }
        });
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return ResourceLocation.fromNamespaceAndPath("loaderbridge", "server_resources");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager manager) {
                        System.out.println("LOADERBRIDGE_FABRIC_RESOURCE_RELOADED:"
                                + RESOURCE_RELOADS.incrementAndGet());
                    }
                });
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (isTestChunk(chunk) && CHUNK_LOADED.compareAndSet(false, true)) {
                System.out.println("LOADERBRIDGE_FABRIC_CHUNK_LOADED");
            }
        });
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            if (isTestChunk(chunk) && CHUNK_GENERATED.compareAndSet(false, true)) {
                System.out.println("LOADERBRIDGE_FABRIC_CHUNK_GENERATED");
            }
        });
        ServerChunkEvents.CHUNK_LEVEL_TYPE_CHANGE.register((world, chunk, previous, current) -> {
            if (isTestChunk(chunk) && previous == FullChunkStatus.INACCESSIBLE
                    && current == FullChunkStatus.FULL && CHUNK_FULL.compareAndSet(false, true)) {
                System.out.println("LOADERBRIDGE_FABRIC_CHUNK_LEVEL:INACCESSIBLE->FULL");
            }
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            if (isTestChunk(chunk) && CHUNK_UNLOADED.compareAndSet(false, true)) {
                System.out.println("LOADERBRIDGE_FABRIC_CHUNK_UNLOADED");
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, world) -> {
            if (entity.getType() == BlockEntityType.CHEST) {
                System.out.println("LOADERBRIDGE_FABRIC_BLOCK_ENTITY_LOADED");
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, world) -> {
            if (entity.getType() == BlockEntityType.CHEST) {
                System.out.println("LOADERBRIDGE_FABRIC_BLOCK_ENTITY_UNLOADED");
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity.getTags().contains("loaderbridge_entity_fixture")) {
                System.out.println("LOADERBRIDGE_FABRIC_ENTITY_LOADED");
            }
        });
        ServerEntityEvents.EQUIPMENT_CHANGE.register((entity, slot, previous, current) -> {
            if (entity.getTags().contains("loaderbridge_entity_fixture")
                    && slot == EquipmentSlot.HEAD && current.is(Items.DIAMOND_HELMET)) {
                System.out.println("LOADERBRIDGE_FABRIC_EQUIPMENT_CHANGED");
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity.getTags().contains("loaderbridge_entity_fixture")) {
                System.out.println("LOADERBRIDGE_FABRIC_ENTITY_UNLOADED");
            }
        });
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (WORLDS_LOADED.incrementAndGet() == 3) {
                System.out.println("LOADERBRIDGE_FABRIC_WORLDS_LOADED");
            }
        });
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (WORLDS_UNLOADED.incrementAndGet() == 3) {
                System.out.println("LOADERBRIDGE_FABRIC_WORLDS_UNLOADED");
            }
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> SERVER_STATE.compareAndSet(0, 1));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            String dynamicValue = server.registryAccess().registryOrThrow(DYNAMIC_REGISTRY_KEY)
                    .get(ResourceLocation.fromNamespaceAndPath("loaderbridge", "value"));
            if (!"dynamic-value".equals(dynamicValue)
                    || !DYNAMIC_REGISTRY_SETUP.get()
                    || !DYNAMIC_REGISTRY_CALLBACK.get()
                    || server.registryAccess().registryOrThrow(EMPTY_DYNAMIC_REGISTRY_KEY).size() != 0) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_DYNAMIC_REGISTRY_FAILED");
            }
            System.out.println("LOADERBRIDGE_FABRIC_DYNAMIC_REGISTRY_SETUP_READY");
            System.out.println("LOADERBRIDGE_FABRIC_DYNAMIC_REGISTRY_READY");
            if (!DefaultAttributes.hasSupplier(attributeFixtureType)) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_DEFAULT_ATTRIBUTES_FAILED");
            }
            System.out.println("LOADERBRIDGE_FABRIC_DEFAULT_ATTRIBUTES_READY");
            var world = server.overworld();
            BlockPos lookupPos = new BlockPos(world.getSharedSpawnPos().getX(),
                    world.getMinBuildHeight() + 1, world.getSharedSpawnPos().getZ());
            world.setBlockAndUpdate(lookupPos, Blocks.STONE.defaultBlockState());
            BlockApiCache<String, Void> lookupCache = BlockApiCache.create(
                    BLOCK_LOOKUP, world, lookupPos);
            if (!"direct".equals(BLOCK_LOOKUP.find(world, lookupPos, null))
                    || !"direct".equals(lookupCache.find(null))) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_BLOCK_LOOKUP_DIRECT_FAILED");
            }
            world.setBlockAndUpdate(lookupPos, Blocks.DIRT.defaultBlockState());
            if (!"fallback".equals(lookupCache.find(null))) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_BLOCK_LOOKUP_FALLBACK_FAILED");
            }
            System.out.println("LOADERBRIDGE_FABRIC_BLOCK_LOOKUP_READY");
            ItemStack diamond = new ItemStack(Items.DIAMOND);
            if (!"direct".equals(ITEM_LOOKUP.find(diamond, null))
                    || ITEM_SELF_LOOKUP.find(diamond, null) != Items.DIAMOND
                    || !"fallback".equals(ITEM_LOOKUP.find(new ItemStack(Items.DIRT), null))) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_ITEM_LOOKUP_FAILED");
            }
            System.out.println("LOADERBRIDGE_FABRIC_ITEM_LOOKUP_READY");
            BlockPos chestPos = lookupPos.above();
            world.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
            if (!(world.getBlockEntity(chestPos) instanceof net.minecraft.world.Container chest)) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_ITEM_SIDED_CHEST_FAILED");
            }
            chest.clearContent();
            Storage<ItemVariant> discoveredStorage =
                    ItemStorage.SIDED.find(world, chestPos, Direction.UP);
            try (Transaction committed = Transaction.openOuter()) {
                if (discoveredStorage == null
                        || discoveredStorage.insert(ItemVariant.of(Items.DIAMOND), 3, committed) != 3) {
                    throw new IllegalStateException("LOADERBRIDGE_FABRIC_ITEM_SIDED_INSERT_FAILED");
                }
                committed.commit();
            }
            if (chest.getItem(0).getCount() != 3) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_ITEM_SIDED_STORAGE_FAILED");
            }
            System.out.println("LOADERBRIDGE_FABRIC_ITEM_SIDED_LOOKUP_READY");
            Entity armorStand = EntityType.ARMOR_STAND.create(world);
            Entity zombie = EntityType.ZOMBIE.create(world);
            if (armorStand == null || zombie == null
                    || !"direct".equals(ENTITY_LOOKUP.find(armorStand, null))
                    || ENTITY_SELF_LOOKUP.find(armorStand, null) != armorStand
                    || !"fallback".equals(ENTITY_LOOKUP.find(zombie, null))) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_ENTITY_LOOKUP_FAILED");
            }
            System.out.println("LOADERBRIDGE_FABRIC_ENTITY_API_LOOKUP_READY");
            if (SERVER_STATE.compareAndSet(1, 2)) {
                System.out.println("LOADERBRIDGE_FABRIC_SERVER_LIFECYCLE_READY");
            }
            if (server.getGameRules().getRule(PERSISTED_RULE).get()) {
                System.out.println("LOADERBRIDGE_FABRIC_GAME_RULE_PERSISTED");
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (SERVER_STATE.compareAndSet(2, 3)) {
                System.out.println("LOADERBRIDGE_FABRIC_SERVER_STOPPING");
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (SERVER_STATE.compareAndSet(3, 4)) {
                System.out.println("LOADERBRIDGE_FABRIC_SERVER_STOPPED");
            }
        });
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resources) ->
                System.out.println("LOADERBRIDGE_FABRIC_RELOAD_STARTED"));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) {
                System.out.println("LOADERBRIDGE_FABRIC_RELOAD_FINISHED");
            }
        });
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) ->
                System.out.println("LOADERBRIDGE_FABRIC_SAVE_STARTED:" + flush + ":" + force));
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) ->
                System.out.println("LOADERBRIDGE_FABRIC_SAVE_FINISHED:" + flush + ":" + force));
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (!client) {
                System.out.println("LOADERBRIDGE_FABRIC_LIFECYCLE_TAGS_READY");
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> STATE.compareAndSet(0, 1));
        ServerTickEvents.START_WORLD_TICK.register(world -> STATE.compareAndSet(1, 2));
        ServerTickEvents.END_WORLD_TICK.register(world -> STATE.compareAndSet(2, 3));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (STATE.compareAndSet(3, 4) && REPORTED.compareAndSet(false, true)) {
                System.out.println("LOADERBRIDGE_FABRIC_LIFECYCLE_TICKS_READY");
            }
        });
    }

    private static boolean isTestChunk(net.minecraft.world.level.chunk.LevelChunk chunk) {
        return chunk.getPos().x == TEST_CHUNK && chunk.getPos().z == TEST_CHUNK;
    }

    private static final class TransactionFixture extends SnapshotParticipant<Integer> {
        private int value;
        private int commits;

        private void set(int next, TransactionContext transaction) {
            updateSnapshots(transaction);
            value = next;
        }

        @Override
        protected Integer createSnapshot() {
            return value;
        }

        @Override
        protected void readSnapshot(Integer snapshot) {
            value = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            commits++;
        }
    }

    private static final class TransactionalStorage extends SnapshotParticipant<Long>
            implements SingleSlotStorage<String> {
        private long amount;

        @Override
        public long insert(String resource, long maximum, TransactionContext transaction) {
            if (!"energy".equals(resource) || maximum < 0) return 0;
            long inserted = Math.min(maximum, getCapacity() - amount);
            if (inserted > 0) {
                updateSnapshots(transaction);
                amount += inserted;
            }
            return inserted;
        }

        @Override
        public long extract(String resource, long maximum, TransactionContext transaction) {
            if (!"energy".equals(resource) || maximum < 0) return 0;
            long extracted = Math.min(maximum, amount);
            if (extracted > 0) {
                updateSnapshots(transaction);
                amount -= extracted;
            }
            return extracted;
        }

        @Override
        public java.util.Iterator<StorageView<String>> iterator() {
            return java.util.Collections.<StorageView<String>>singleton(this).iterator();
        }

        @Override
        public boolean isResourceBlank() {
            return amount == 0;
        }

        @Override
        public String getResource() {
            return "energy";
        }

        @Override
        public long getAmount() {
            return amount;
        }

        @Override
        public long getCapacity() {
            return 100;
        }

        @Override
        protected Long createSnapshot() {
            return amount;
        }

        @Override
        protected void readSnapshot(Long snapshot) {
            amount = snapshot;
        }
    }
}
