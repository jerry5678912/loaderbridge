package net.fabricmc.fabric.impl.attachment.sync;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public interface SupportedAttachmentsConnection {
    void loaderbridge$setSupportedAttachments(Set<ResourceLocation> attachments);
    Set<ResourceLocation> loaderbridge$getSupportedAttachments();
}
