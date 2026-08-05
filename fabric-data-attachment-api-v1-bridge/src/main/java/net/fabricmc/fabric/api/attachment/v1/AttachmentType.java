package net.fabricmc.fabric.api.attachment.v1;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

public interface AttachmentType<A> {
    ResourceLocation identifier();
    Codec<A> persistenceCodec();
    default boolean isPersistent() { return persistenceCodec() != null; }
    Supplier<A> initializer();
    boolean isSynced();
    boolean copyOnDeath();
}
