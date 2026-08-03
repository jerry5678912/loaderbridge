package net.fabricmc.fabric.api.resource;

import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.server.packs.PackResources;

public interface ModResourcePack extends PackResources {
    ModMetadata getFabricModMetadata();

    ModResourcePack createOverlay(String overlay);
}
