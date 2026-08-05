package net.fabricmc.fabric.api.attachment.v1;

import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface AttachmentSyncPredicate extends BiPredicate<AttachmentTarget, ServerPlayer> {
    static AttachmentSyncPredicate all() { return (target, player) -> true; }
    static AttachmentSyncPredicate targetOnly() { return (target, player) -> target == player; }
    static AttachmentSyncPredicate allButTarget() { return (target, player) -> target != player; }
}
