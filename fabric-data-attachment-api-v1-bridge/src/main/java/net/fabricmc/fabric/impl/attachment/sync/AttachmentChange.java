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
    private static final System.Logger LOGGER = System.getLogger(AttachmentChange.class.getName());
    private static final boolean DISCONNECT_ON_UNKNOWN_TARGETS =
            System.getProperty("fabric.attachment.disconnect_on_unknown_targets") != null;
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
    public boolean apply(Level level) {
        AttachmentTarget target = targetInfo.resolve(level);
        if (target == null) {
            handleUnknownTarget(targetInfo, type, DISCONNECT_ON_UNKNOWN_TARGETS);
            return false;
        }
        target.setAttached((AttachmentType<Object>) type, value);
        return true;
    }

    public static void handleUnknownTarget(AttachmentTargetInfo targetInfo,
            AttachmentType<?> type, boolean disconnect) {
        String message = "LB-ATTACH-006: unknown synchronized target " + targetInfo
                + " for attachment '" + type.identifier() + "'";
        if (disconnect) throw new IllegalStateException(message);
        LOGGER.log(System.Logger.Level.WARNING, message);
    }
}
