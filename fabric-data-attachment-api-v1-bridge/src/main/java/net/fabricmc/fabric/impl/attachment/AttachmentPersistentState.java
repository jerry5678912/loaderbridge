package net.fabricmc.fabric.impl.attachment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class AttachmentPersistentState extends SavedData {
    public static final String ID = "fabric_attachments";
    private final AttachmentTargetImpl levelTarget;
    private final boolean wasSerialized;

    private AttachmentPersistentState(ServerLevel level) {
        levelTarget = (AttachmentTargetImpl) (Object) level;
        wasSerialized = levelTarget.fabric_hasPersistentAttachments();
    }

    public static AttachmentPersistentState getOrCreate(ServerLevel level) {
        SavedData.Factory<AttachmentPersistentState> factory = new SavedData.Factory<>(
                () -> new AttachmentPersistentState(level),
                (tag, registries) -> read(level, tag, registries), null);
        return level.getDataStorage().computeIfAbsent(factory, ID);
    }

    private static AttachmentPersistentState read(ServerLevel level, CompoundTag tag,
            HolderLookup.Provider registries) {
        AttachmentTargetImpl target = (AttachmentTargetImpl) (Object) level;
        target.fabric_readAttachments(tag,
                registries.createSerializationContext(NbtOps.INSTANCE));
        return new AttachmentPersistentState(level);
    }

    @Override public boolean isDirty() {
        return super.isDirty() || wasSerialized || levelTarget.fabric_hasPersistentAttachments();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        levelTarget.fabric_writeAttachments(tag,
                registries.createSerializationContext(NbtOps.INSTANCE));
        return tag;
    }
}
