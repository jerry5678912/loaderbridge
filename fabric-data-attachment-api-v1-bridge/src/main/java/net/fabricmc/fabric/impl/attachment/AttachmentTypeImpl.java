package net.fabricmc.fabric.impl.attachment;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record AttachmentTypeImpl<A>(ResourceLocation identifier, Supplier<A> initializer,
        Codec<A> persistenceCodec,
        StreamCodec<? super RegistryFriendlyByteBuf, A> packetCodec,
        AttachmentSyncPredicate syncPredicate, boolean copyOnDeath) implements AttachmentType<A> {
    @Override public boolean isSynced() { return syncPredicate != null; }
}
