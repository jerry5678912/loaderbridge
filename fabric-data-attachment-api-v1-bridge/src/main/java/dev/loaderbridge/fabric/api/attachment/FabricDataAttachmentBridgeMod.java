package dev.loaderbridge.fabric.api.attachment;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentSyncRuntime;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentNegotiation;
import net.fabricmc.fabric.impl.attachment.sync.s2c.AttachmentSyncPayloadS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("loaderbridge_fabric_data_attachment_api_v1")
public final class FabricDataAttachmentBridgeMod {
    public FabricDataAttachmentBridgeMod() {
        AttachmentNegotiation.initialize();
        PayloadTypeRegistry.playS2C().register(
                AttachmentSyncPayloadS2C.ID, AttachmentSyncPayloadS2C.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            AttachmentSyncRuntime.syncInitial((AttachmentTarget) (Object) player.serverLevel(), player);
            AttachmentSyncRuntime.syncInitial((AttachmentTarget) (Object) player, player);
        });
        EntityTrackingEvents.START_TRACKING.register((entity, player) ->
                AttachmentSyncRuntime.syncInitial((AttachmentTarget) (Object) entity, player));
        MinecraftForge.EVENT_BUS.addListener(this::onChunkWatch);
        MinecraftForge.EVENT_BUS.addListener(AttachmentNegotiation::gatherLoginTask);
        if (FMLEnvironment.dist == Dist.CLIENT) FabricDataAttachmentClientHooks.register();
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                AttachmentTargetImpl.transfer((AttachmentTarget) (Object) oldPlayer,
                        (AttachmentTarget) (Object) newPlayer, !alive));
        ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register(
                (original, replacement, origin, destination) ->
                        AttachmentTargetImpl.transfer((AttachmentTarget) (Object) original,
                                (AttachmentTarget) (Object) replacement, false));
        ServerLivingEntityEvents.MOB_CONVERSION.register((original, replacement, keepEquipment) ->
                AttachmentTargetImpl.transfer((AttachmentTarget) (Object) original,
                        (AttachmentTarget) (Object) replacement, true));
    }

    private void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerPlayer player = event.getPlayer();
        AttachmentSyncRuntime.syncInitial((AttachmentTarget) (Object) event.getChunk(), player);
        for (BlockEntity blockEntity : event.getChunk().getBlockEntities().values()) {
            AttachmentSyncRuntime.syncInitial((AttachmentTarget) (Object) blockEntity, player);
        }
    }
}
