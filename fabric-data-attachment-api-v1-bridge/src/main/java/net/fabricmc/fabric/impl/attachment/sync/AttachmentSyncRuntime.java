package net.fabricmc.fabric.impl.attachment.sync;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.fabricmc.fabric.impl.attachment.AttachmentTypeImpl;
import net.fabricmc.fabric.impl.attachment.sync.s2c.AttachmentSyncPayloadS2C;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class AttachmentSyncRuntime {
    private AttachmentSyncRuntime() { }

    public static void syncChange(AttachmentTarget target, AttachmentType<?> type, Object value) {
        if (!type.isSynced()) return;
        AttachmentTargetInfo targetInfo = AttachmentTargetInfo.of(target);
        if (targetInfo == null) return;
        var payload = new AttachmentSyncPayloadS2C(List.of(
                new AttachmentChange(targetInfo, type, value)));
        for (ServerPlayer player : recipients(target)) {
            if (shouldSend(target, type, player)) ServerPlayNetworking.send(player, payload);
        }
    }

    public static void syncInitial(AttachmentTarget target, ServerPlayer player) {
        var attachments = ((AttachmentTargetImpl) target).fabric_getAttachments();
        AttachmentTargetInfo targetInfo = AttachmentTargetInfo.of(target);
        if (attachments == null || targetInfo == null) return;
        var changes = attachments.entrySet().stream()
                .filter(entry -> shouldSend(target, entry.getKey(), player))
                .map(entry -> new AttachmentChange(targetInfo, entry.getKey(), entry.getValue()))
                .toList();
        if (!changes.isEmpty()) {
            ServerPlayNetworking.send(player, new AttachmentSyncPayloadS2C(changes));
        }
    }

    private static boolean shouldSend(AttachmentTarget target, AttachmentType<?> type,
            ServerPlayer player) {
        if (!type.isSynced() || !ServerPlayNetworking.canSend(player,
                AttachmentSyncPayloadS2C.ID)) return false;
        return ((AttachmentTypeImpl<?>) type).syncPredicate().test(target, player);
    }

    private static Collection<ServerPlayer> recipients(AttachmentTarget target) {
        Object value = target;
        LinkedHashSet<ServerPlayer> players = new LinkedHashSet<>();
        if (value instanceof ServerPlayer self) players.add(self);
        if (value instanceof Entity entity && entity.level() instanceof ServerLevel) {
            players.addAll(PlayerLookup.tracking(entity));
        } else if (value instanceof BlockEntity blockEntity && blockEntity.hasLevel()
                && blockEntity.getLevel() instanceof ServerLevel) {
            players.addAll(PlayerLookup.tracking(blockEntity));
        } else if (value instanceof LevelChunk chunk
                && chunk.getLevel() instanceof ServerLevel level) {
            players.addAll(PlayerLookup.tracking(level, chunk.getPos()));
        } else if (value instanceof ServerLevel level) {
            players.addAll(PlayerLookup.world(level));
        }
        return players;
    }
}
