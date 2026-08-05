package net.fabricmc.fabric.impl.attachment;

import com.mojang.serialization.DynamicOps;
import java.util.IdentityHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public interface AttachmentTargetImpl extends AttachmentTarget {
    static void transfer(AttachmentTarget original, AttachmentTarget target, boolean isDeath) {
        IdentityHashMap<AttachmentType<?>, Object> attachments =
                ((AttachmentTargetImpl) original).fabric_getAttachments();
        if (attachments == null) return;
        for (var entry : attachments.entrySet()) {
            transferEntry(target, entry.getKey(), entry.getValue(), isDeath);
        }
    }

    @SuppressWarnings("unchecked")
    private static <A> void transferEntry(AttachmentTarget target, AttachmentType<?> rawType,
            Object value, boolean isDeath) {
        AttachmentType<A> type = (AttachmentType<A>) rawType;
        if (!isDeath || type.copyOnDeath()) target.setAttached(type, (A) value);
    }

    IdentityHashMap<AttachmentType<?>, Object> fabric_getAttachments();
    void fabric_setAttachments(IdentityHashMap<AttachmentType<?>, Object> attachments);

    default void fabric_writeAttachments(CompoundTag tag, DynamicOps<Tag> ops) {
        AttachmentSerialization.write(tag, ops, fabric_getAttachments());
    }

    default void fabric_readAttachments(CompoundTag tag, DynamicOps<Tag> ops) {
        fabric_setAttachments(AttachmentSerialization.read(tag, ops));
    }

    default boolean fabric_hasPersistentAttachments() {
        return AttachmentSerialization.hasPersistent(fabric_getAttachments());
    }
}
