package net.fabricmc.fabric.api.screenhandler.v1;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/** A menu provider that supplies additional data for the opening packet. */
public interface ExtendedScreenHandlerFactory<D> extends MenuProvider {
    D getScreenOpeningData(ServerPlayer player);
}
