package net.fabricmc.fabric.impl.attachment.sync.s2c;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentChange;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AttachmentSyncPayloadS2C(List<AttachmentChange> attachments)
        implements CustomPacketPayload {
    public static final int MAX_PAYLOAD_SIZE = 1_048_576;
    private static final int MAX_CHANGES = 4096;
    public static final Type<AttachmentSyncPayloadS2C> ID = new Type<>(
            ResourceLocation.fromNamespaceAndPath("fabric", "attachment_sync_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AttachmentSyncPayloadS2C> CODEC =
            StreamCodec.ofMember(AttachmentSyncPayloadS2C::encode, AttachmentSyncPayloadS2C::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(attachments.size());
        attachments.forEach(change -> change.encode(buffer));
    }

    private static AttachmentSyncPayloadS2C decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_CHANGES) {
            throw new IllegalArgumentException("LB-ATTACH-004: invalid attachment batch " + size);
        }
        List<AttachmentChange> changes = new ArrayList<>(size);
        for (int index = 0; index < size; index++) changes.add(AttachmentChange.decode(buffer));
        return new AttachmentSyncPayloadS2C(List.copyOf(changes));
    }

    public int encodedSize(RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registryAccess);
        try {
            CODEC.encode(buffer, this);
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }

    public static List<AttachmentSyncPayloadS2C> partition(
            List<AttachmentChange> changes, RegistryAccess registryAccess) {
        return partition(changes, registryAccess, MAX_PAYLOAD_SIZE);
    }

    public static List<AttachmentSyncPayloadS2C> partition(
            List<AttachmentChange> changes, RegistryAccess registryAccess,
            int maximumPayloadSize) {
        if (maximumPayloadSize <= 0) {
            throw new IllegalArgumentException(
                    "LB-ATTACH-005: maximum payload size must be positive");
        }
        List<SizedChange> sorted = changes.stream()
                .map(change -> new SizedChange(change, new AttachmentSyncPayloadS2C(
                        List.of(change)).encodedSize(registryAccess) - varIntSize(1)))
                .sorted(Comparator.comparingInt(SizedChange::entrySize))
                .toList();
        List<AttachmentSyncPayloadS2C> packets = new ArrayList<>();
        List<AttachmentChange> current = new ArrayList<>();
        int currentEntryBytes = 0;
        for (SizedChange sized : sorted) {
            int singleSize = varIntSize(1) + sized.entrySize();
            if (singleSize > maximumPayloadSize) {
                throw new IllegalArgumentException("LB-ATTACH-005: data for attachment '"
                        + sized.change().type().identifier() + "' is too large ("
                        + singleSize + " bytes, maximum " + maximumPayloadSize + ")");
            }
            int candidateCount = current.size() + 1;
            int candidateSize = varIntSize(candidateCount) + currentEntryBytes
                    + sized.entrySize();
            if (!current.isEmpty() && (candidateCount > MAX_CHANGES
                    || candidateSize > maximumPayloadSize)) {
                packets.add(new AttachmentSyncPayloadS2C(List.copyOf(current)));
                current.clear();
                currentEntryBytes = 0;
            }
            current.add(sized.change());
            currentEntryBytes += sized.entrySize();
        }
        if (!current.isEmpty()) {
            packets.add(new AttachmentSyncPayloadS2C(List.copyOf(current)));
        }
        return List.copyOf(packets);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    private static int varIntSize(int value) {
        int size = 1;
        while ((value & ~0x7F) != 0) {
            size++;
            value >>>= 7;
        }
        return size;
    }

    private record SizedChange(AttachmentChange change, int entrySize) { }
}
