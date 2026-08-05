package dev.loaderbridge.fabric.api.attachment.mixin;

import java.util.IdentityHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.fabricmc.fabric.impl.attachment.AttachmentPersistentState;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentSyncRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({BlockEntity.class, Entity.class, Level.class, ChunkAccess.class})
abstract class AttachmentTargetsMixin implements AttachmentTargetImpl {
    @Unique private IdentityHashMap<AttachmentType<?>, Object> loaderbridge$attachments;

    @Override @SuppressWarnings("unchecked")
    public <A> A getAttached(AttachmentType<A> type) {
        return loaderbridge$attachments == null
                ? null : (A) loaderbridge$attachments.get(type);
    }

    @Override @SuppressWarnings("unchecked")
    public <A> A setAttached(AttachmentType<A> type, A value) {
        loaderbridge$markChanged();
        A previous;
        if (value == null) {
            previous = loaderbridge$attachments == null
                    ? null : (A) loaderbridge$attachments.remove(type);
        } else {
            if (loaderbridge$attachments == null) loaderbridge$attachments = new IdentityHashMap<>();
            previous = (A) loaderbridge$attachments.put(type, value);
        }
        AttachmentSyncRuntime.syncChange(this, type, value);
        return previous;
    }

    @Override public boolean hasAttached(AttachmentType<?> type) {
        return loaderbridge$attachments != null && loaderbridge$attachments.containsKey(type);
    }

    @Override public IdentityHashMap<AttachmentType<?>, Object> fabric_getAttachments() {
        return loaderbridge$attachments;
    }

    @Override public void fabric_setAttachments(
            IdentityHashMap<AttachmentType<?>, Object> attachments) {
        loaderbridge$attachments = attachments == null || attachments.isEmpty()
                ? null : attachments;
    }

    @Unique
    private void loaderbridge$markChanged() {
        Object target = this;
        if (target instanceof BlockEntity blockEntity) blockEntity.setChanged();
        if (target instanceof ChunkAccess chunk) chunk.setUnsaved(true);
        if (target instanceof ServerLevel level) AttachmentPersistentState.getOrCreate(level).setDirty();
    }
}
