package net.fabricmc.fabric.impl.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AttachmentSerialization {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            "fabric-data-attachment-api-v1");

    private AttachmentSerialization() { }

    @SuppressWarnings("unchecked")
    public static void write(CompoundTag root, DynamicOps<Tag> ops,
            IdentityHashMap<AttachmentType<?>, ?> attachments) {
        if (attachments == null || attachments.isEmpty()) return;
        CompoundTag encodedAttachments = new CompoundTag();
        for (Map.Entry<AttachmentType<?>, ?> entry : attachments.entrySet()) {
            AttachmentType<?> type = entry.getKey();
            Codec<Object> codec = (Codec<Object>) type.persistenceCodec();
            if (codec == null) continue;
            codec.encodeStart(ops, entry.getValue())
                    .resultOrPartial(message -> LOGGER.warn(
                            "Couldn't serialize attachment {}, skipping: {}",
                            type.identifier(), message))
                    .ifPresent(tag -> encodedAttachments.put(type.identifier().toString(), tag));
        }
        root.put(AttachmentTarget.NBT_ATTACHMENT_KEY, encodedAttachments);
    }

    public static IdentityHashMap<AttachmentType<?>, Object> read(
            CompoundTag root, DynamicOps<Tag> ops) {
        IdentityHashMap<AttachmentType<?>, Object> attachments = new IdentityHashMap<>();
        if (!root.contains(AttachmentTarget.NBT_ATTACHMENT_KEY, Tag.TAG_COMPOUND)) {
            return attachments;
        }
        CompoundTag encodedAttachments = root.getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY);
        for (String key : encodedAttachments.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            AttachmentType<?> type = id == null ? null : AttachmentRegistryImpl.get(id);
            if (type == null) {
                LOGGER.warn("Unknown attachment type {} found when deserializing, skipping", key);
                continue;
            }
            decode(type, encodedAttachments.get(key), ops, attachments);
        }
        return attachments;
    }

    @SuppressWarnings("unchecked")
    private static <A> void decode(AttachmentType<A> type, Tag tag, DynamicOps<Tag> ops,
            IdentityHashMap<AttachmentType<?>, Object> output) {
        Codec<A> codec = type.persistenceCodec();
        if (codec == null || tag == null) return;
        codec.parse(ops, tag)
                .resultOrPartial(message -> LOGGER.warn(
                        "Couldn't deserialize attachment {}, skipping: {}",
                        type.identifier(), message))
                .ifPresent(value -> output.put(type, value));
    }

    public static boolean hasPersistent(IdentityHashMap<AttachmentType<?>, ?> attachments) {
        if (attachments == null) return false;
        return attachments.keySet().stream().anyMatch(AttachmentType::isPersistent);
    }
}
