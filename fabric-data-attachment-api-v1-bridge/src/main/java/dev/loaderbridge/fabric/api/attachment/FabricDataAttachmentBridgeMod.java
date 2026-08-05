package dev.loaderbridge.fabric.api.attachment;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraftforge.fml.common.Mod;

@Mod("loaderbridge_fabric_data_attachment_api_v1")
public final class FabricDataAttachmentBridgeMod {
    public FabricDataAttachmentBridgeMod() {
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
}
