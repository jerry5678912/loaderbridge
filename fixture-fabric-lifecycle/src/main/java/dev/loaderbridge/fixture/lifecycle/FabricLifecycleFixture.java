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
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.Heightmap;

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

    @Override
    @SuppressWarnings("deprecation")
    public void onInitialize() {
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
}
