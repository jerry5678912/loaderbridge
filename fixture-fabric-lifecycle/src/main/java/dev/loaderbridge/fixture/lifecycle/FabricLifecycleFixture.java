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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

/** Verifies Forge-to-Fabric server and world tick ordering at runtime. */
public final class FabricLifecycleFixture implements ModInitializer {
    private static final AtomicInteger STATE = new AtomicInteger();
    private static final AtomicInteger SERVER_STATE = new AtomicInteger();
    private static final AtomicInteger WORLDS_LOADED = new AtomicInteger();
    private static final AtomicInteger WORLDS_UNLOADED = new AtomicInteger();
    private static final AtomicBoolean REPORTED = new AtomicBoolean();
    private static final AtomicBoolean CHUNK_LOADED = new AtomicBoolean();
    private static final AtomicBoolean CHUNK_GENERATED = new AtomicBoolean();
    private static final AtomicBoolean CHUNK_FULL = new AtomicBoolean();
    private static final AtomicBoolean CHUNK_UNLOADED = new AtomicBoolean();
    private static final AtomicInteger RESOURCE_RELOADS = new AtomicInteger();
    private static final int TEST_CHUNK = 700;

    @Override
    public void onInitialize() {
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
            if (SERVER_STATE.compareAndSet(1, 2)) {
                System.out.println("LOADERBRIDGE_FABRIC_SERVER_LIFECYCLE_READY");
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
