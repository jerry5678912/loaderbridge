package net.fabricmc.fabric.impl.recipe.ingredient;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;

public final class IngredientSyncTask implements ConfigurationTask {
    public static final Type TYPE = new Type("loaderbridge:fabric_custom_ingredient_sync");

    @Override public void start(Consumer<Packet<?>> sender) {
        sender.accept(ServerConfigurationNetworking.createS2CPacket(
                new CustomIngredientQueryS2C(CustomIngredientNegotiation.PROTOCOL_VERSION)));
    }

    @Override public Type type() { return TYPE; }
}
