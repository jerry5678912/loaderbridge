package dev.loaderbridge.fabric.api.attachment.mixin;

import java.util.Set;
import net.fabricmc.fabric.impl.attachment.sync.SupportedAttachmentsConnection;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public abstract class ConnectionMixin implements SupportedAttachmentsConnection {
    @Unique private Set<ResourceLocation> loaderbridge$supportedAttachments = Set.of();

    @Override public void loaderbridge$setSupportedAttachments(
            Set<ResourceLocation> attachments) {
        loaderbridge$supportedAttachments = Set.copyOf(attachments);
    }

    @Override public Set<ResourceLocation> loaderbridge$getSupportedAttachments() {
        return loaderbridge$supportedAttachments;
    }
}
