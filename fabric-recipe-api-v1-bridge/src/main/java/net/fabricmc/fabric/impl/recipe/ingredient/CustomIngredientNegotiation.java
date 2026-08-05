package net.fabricmc.fabric.impl.recipe.ingredient;

import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;

public final class CustomIngredientNegotiation {
    private static final System.Logger LOGGER = System.getLogger(
            CustomIngredientNegotiation.class.getName());
    public static final ResourceLocation CHANNEL_C2S = ResourceLocation.fromNamespaceAndPath(
            "loaderbridge", "fabric_custom_ingredient_sync_c2s");
    public static final ResourceLocation CHANNEL_S2C = ResourceLocation.fromNamespaceAndPath(
            "loaderbridge", "fabric_custom_ingredient_sync_s2c");
    public static final int PROTOCOL_VERSION = 1;
    public static final ThreadLocal<Set<ResourceLocation>> CURRENT_SUPPORTED_SERIALIZERS =
            new ThreadLocal<>();

    public static void initialize() {
        PayloadTypeRegistry.configurationC2S().register(
                CustomIngredientSupportC2S.TYPE, CustomIngredientSupportC2S.CODEC);
        PayloadTypeRegistry.configurationS2C().register(
                CustomIngredientQueryS2C.TYPE, CustomIngredientQueryS2C.CODEC);
        ServerConfigurationNetworking.registerGlobalReceiver(
                CustomIngredientSupportC2S.TYPE, (payload, context) -> {
                    Set<ResourceLocation> supported = payload.protocolVersion() == PROTOCOL_VERSION
                            ? payload.registeredSerializers().stream()
                                    .filter(CustomIngredientImpl.registeredSerializerIds()::contains)
                                    .collect(java.util.stream.Collectors.toUnmodifiableSet())
                            : Set.of();
                    Connection connection = context.networkHandler().getConnection();
                    ((SupportedIngredientsConnection) connection)
                            .loaderbridge$setSupportedCustomIngredients(supported);
                    LOGGER.log(System.Logger.Level.INFO,
                            "LoaderBridge negotiated {0} Fabric custom ingredient serializers",
                            supported.size());
                    context.networkHandler().finishCurrentTask(IngredientSyncTask.TYPE);
                });
    }

    public static void initializeClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking
                .registerGlobalReceiver(CustomIngredientQueryS2C.TYPE, (payload, context) -> {
                    if (payload.protocolVersion() < PROTOCOL_VERSION) return;
                    context.responseSender().sendPacket(new CustomIngredientSupportC2S(
                            PROTOCOL_VERSION, CustomIngredientImpl.registeredSerializerIds()));
                });
    }

    public static void gatherLoginTask(GatherLoginConfigurationTasksEvent event) {
        Connection connection = event.getConnection();
        boolean supported = dev.loaderbridge.fabric.api.networking.NetworkBridgeRuntime
                .remoteConfigurationS2CChannels(connection)
                .contains(CHANNEL_S2C);
        if (!supported) {
            ((SupportedIngredientsConnection) connection)
                    .loaderbridge$setSupportedCustomIngredients(Set.of());
            LOGGER.log(System.Logger.Level.INFO,
                    "LoaderBridge will use vanilla ingredient fallback for this client");
            return;
        }
        event.addTask(new IngredientSyncTask());
    }

    public static boolean shouldFallback(
            net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient ingredient) {
        if (ingredient == null) return false;
        Set<ResourceLocation> supported = CURRENT_SUPPORTED_SERIALIZERS.get();
        return supported != null && !supported.contains(ingredient.getSerializer().getIdentifier());
    }

    private CustomIngredientNegotiation() { }
}
