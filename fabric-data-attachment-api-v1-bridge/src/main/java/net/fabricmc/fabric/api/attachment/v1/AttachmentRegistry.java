package net.fabricmc.fabric.api.attachment.v1;

import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class AttachmentRegistry {
    private AttachmentRegistry() { }

    public static <A> AttachmentType<A> create(ResourceLocation id, Consumer<Builder<A>> consumer) {
        Builder<A> builder = AttachmentRegistryImpl.builder();
        consumer.accept(builder);
        return builder.buildAndRegister(id);
    }

    public static <A> AttachmentType<A> create(ResourceLocation id) {
        return create(id, builder -> { });
    }

    public static <A> AttachmentType<A> createDefaulted(
            ResourceLocation id, Supplier<A> initializer) {
        return create(id, builder -> builder.initializer(initializer));
    }

    public static <A> AttachmentType<A> createPersistent(ResourceLocation id, Codec<A> codec) {
        return create(id, builder -> builder.persistent(codec));
    }

    @Deprecated
    public static <A> Builder<A> builder() { return AttachmentRegistryImpl.builder(); }

    public interface Builder<A> {
        Builder<A> persistent(Codec<A> codec);
        Builder<A> copyOnDeath();
        Builder<A> initializer(Supplier<A> initializer);
        Builder<A> syncWith(StreamCodec<? super RegistryFriendlyByteBuf, A> packetCodec,
                AttachmentSyncPredicate syncPredicate);
        AttachmentType<A> buildAndRegister(ResourceLocation id);
    }
}
