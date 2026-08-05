package net.fabricmc.fabric.impl.attachment;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class AttachmentRegistryImpl {
    private static final int MAX_SYNCED_IDENTIFIER_SIZE = 256;
    private static final Map<ResourceLocation, AttachmentType<?>> TYPES = new HashMap<>();
    private static final Set<ResourceLocation> SYNCABLE = new HashSet<>();
    private static final Set<ResourceLocation> SYNCABLE_VIEW =
            Collections.unmodifiableSet(SYNCABLE);

    private AttachmentRegistryImpl() { }

    public static synchronized <A> void register(ResourceLocation id, AttachmentType<A> type) {
        AttachmentType<?> previous = TYPES.put(id, type);
        if (previous != null && previous.isSynced() && !type.isSynced()) SYNCABLE.remove(id);
        if (type.isSynced()) SYNCABLE.add(id);
    }

    public static synchronized AttachmentType<?> get(ResourceLocation id) { return TYPES.get(id); }
    public static Set<ResourceLocation> getSyncableAttachments() { return SYNCABLE_VIEW; }
    public static <A> AttachmentRegistry.Builder<A> builder() { return new BuilderImpl<>(); }

    public static final class BuilderImpl<A> implements AttachmentRegistry.Builder<A> {
        private Supplier<A> initializer;
        private Codec<A> persistenceCodec;
        private StreamCodec<? super RegistryFriendlyByteBuf, A> packetCodec;
        private AttachmentSyncPredicate syncPredicate;
        private boolean copyOnDeath;

        @Override public AttachmentRegistry.Builder<A> persistent(Codec<A> codec) {
            persistenceCodec = Objects.requireNonNull(codec, "codec cannot be null");
            return this;
        }

        @Override public AttachmentRegistry.Builder<A> copyOnDeath() {
            copyOnDeath = true;
            return this;
        }

        @Override public AttachmentRegistry.Builder<A> initializer(Supplier<A> value) {
            initializer = Objects.requireNonNull(value, "initializer cannot be null");
            return this;
        }

        @Override public AttachmentRegistry.Builder<A> syncWith(
                StreamCodec<? super RegistryFriendlyByteBuf, A> codec,
                AttachmentSyncPredicate predicate) {
            packetCodec = Objects.requireNonNull(codec, "packet codec cannot be null");
            syncPredicate = Objects.requireNonNull(predicate, "sync predicate cannot be null");
            return this;
        }

        @Override public AttachmentType<A> buildAndRegister(ResourceLocation id) {
            Objects.requireNonNull(id, "identifier cannot be null");
            if (syncPredicate != null && id.toString().length() > MAX_SYNCED_IDENTIFIER_SIZE) {
                throw new IllegalArgumentException("Identifier length is too long for a synced "
                        + "attachment type (was %d, maximum is %d)".formatted(
                                id.toString().length(), MAX_SYNCED_IDENTIFIER_SIZE));
            }
            AttachmentType<A> type = new AttachmentTypeImpl<>(id, initializer,
                    persistenceCodec, packetCodec, syncPredicate, copyOnDeath);
            register(id, type);
            return type;
        }
    }
}
