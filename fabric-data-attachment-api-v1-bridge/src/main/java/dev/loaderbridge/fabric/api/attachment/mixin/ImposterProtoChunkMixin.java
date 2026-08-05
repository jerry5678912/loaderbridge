package dev.loaderbridge.fabric.api.attachment.mixin;

import java.util.IdentityHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ImposterProtoChunk.class)
abstract class ImposterProtoChunkMixin implements AttachmentTargetImpl {
    private AttachmentTargetImpl loaderbridge$wrapped() {
        return (AttachmentTargetImpl) (Object)
                ((ImposterProtoChunk) (Object) this).getWrapped();
    }

    @Override public <A> A getAttached(AttachmentType<A> type) {
        return ((AttachmentTarget) loaderbridge$wrapped()).getAttached(type);
    }

    @Override public <A> A setAttached(AttachmentType<A> type, A value) {
        return ((AttachmentTarget) loaderbridge$wrapped()).setAttached(type, value);
    }

    @Override public boolean hasAttached(AttachmentType<?> type) {
        return ((AttachmentTarget) loaderbridge$wrapped()).hasAttached(type);
    }

    @Override public IdentityHashMap<AttachmentType<?>, Object> fabric_getAttachments() {
        return loaderbridge$wrapped().fabric_getAttachments();
    }

    @Override public void fabric_setAttachments(
            IdentityHashMap<AttachmentType<?>, Object> attachments) {
        loaderbridge$wrapped().fabric_setAttachments(attachments);
    }
}
