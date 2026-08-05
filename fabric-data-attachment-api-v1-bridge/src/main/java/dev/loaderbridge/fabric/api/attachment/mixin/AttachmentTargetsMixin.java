package dev.loaderbridge.fabric.api.attachment.mixin;

import java.util.IdentityHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({BlockEntity.class, Entity.class, Level.class, ChunkAccess.class})
abstract class AttachmentTargetsMixin implements AttachmentTarget {
    @Unique private IdentityHashMap<AttachmentType<?>, Object> loaderbridge$attachments;

    @Override @SuppressWarnings("unchecked")
    public <A> A getAttached(AttachmentType<A> type) {
        return loaderbridge$attachments == null
                ? null : (A) loaderbridge$attachments.get(type);
    }

    @Override @SuppressWarnings("unchecked")
    public <A> A setAttached(AttachmentType<A> type, A value) {
        loaderbridge$markChanged();
        if (value == null) {
            return loaderbridge$attachments == null
                    ? null : (A) loaderbridge$attachments.remove(type);
        }
        if (loaderbridge$attachments == null) loaderbridge$attachments = new IdentityHashMap<>();
        return (A) loaderbridge$attachments.put(type, value);
    }

    @Override public boolean hasAttached(AttachmentType<?> type) {
        return loaderbridge$attachments != null && loaderbridge$attachments.containsKey(type);
    }

    @Unique
    private void loaderbridge$markChanged() {
        Object target = this;
        if (target instanceof BlockEntity blockEntity) blockEntity.setChanged();
        if (target instanceof ChunkAccess chunk) chunk.setUnsaved(true);
    }
}
