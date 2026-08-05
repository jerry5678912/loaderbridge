package net.fabricmc.fabric.impl.attachment.sync;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.fabricmc.fabric.impl.attachment.AttachmentTypeImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record AttachmentChange(AttachmentTargetInfo targetInfo, AttachmentType<?> type,
        Object value) {
    public void encode(RegistryFriendlyByteBuf buffer) {
        targetInfo.encode(buffer);
        buffer.writeResourceLocation(type.identifier());
        buffer.writeBoolean(value != null);
        if (value != null) encodeValue(buffer, type, value);
    }

    public static AttachmentChange decode(RegistryFriendlyByteBuf buffer) {
        AttachmentTargetInfo target = AttachmentTargetInfo.decode(buffer);
        ResourceLocation id = buffer.readResourceLocation();
        AttachmentType<?> type = AttachmentRegistryImpl.get(id);
        if (type == null) throw new IllegalArgumentException(
                "LB-ATTACH-002: unknown synchronized attachment " + id);
        Object value = buffer.readBoolean() ? decodeValue(buffer, type) : null;
        return new AttachmentChange(target, type, value);
    }

    @SuppressWarnings("unchecked")
    private static <A> void encodeValue(RegistryFriendlyByteBuf buffer,
            AttachmentType<?> rawType, Object rawValue) {
        AttachmentTypeImpl<A> type = (AttachmentTypeImpl<A>) rawType;
        if (type.packetCodec() == null) throw new IllegalStateException(
                "LB-ATTACH-003: synchronized attachment has no packet codec");
        type.packetCodec().encode(buffer, (A) rawValue);
    }

    @SuppressWarnings("unchecked")
    private static <A> A decodeValue(RegistryFriendlyByteBuf buffer,
            AttachmentType<?> rawType) {
        AttachmentTypeImpl<A> type = (AttachmentTypeImpl<A>) rawType;
        if (type.packetCodec() == null) throw new IllegalStateException(
                "LB-ATTACH-003: synchronized attachment has no packet codec");
        return type.packetCodec().decode(buffer);
    }

    @SuppressWarnings("unchecked")
    public void apply(Level level) {
        AttachmentTarget target = targetInfo.resolve(level);
        if (target == null) return;
        target.setAttached((AttachmentType<Object>) type, value);
    }
}
