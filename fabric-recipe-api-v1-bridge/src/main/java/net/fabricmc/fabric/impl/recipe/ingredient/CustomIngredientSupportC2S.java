package net.fabricmc.fabric.impl.recipe.ingredient;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CustomIngredientSupportC2S(
        int protocolVersion, Set<ResourceLocation> registeredSerializers)
        implements CustomPacketPayload {
    public static final Type<CustomIngredientSupportC2S> TYPE =
            new Type<>(CustomIngredientNegotiation.CHANNEL_C2S);
    public static final StreamCodec<FriendlyByteBuf, CustomIngredientSupportC2S> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CustomIngredientSupportC2S::protocolVersion,
                    ByteBufCodecs.collection(HashSet::new, ResourceLocation.STREAM_CODEC),
                    CustomIngredientSupportC2S::registeredSerializers,
                    CustomIngredientSupportC2S::new);

    public CustomIngredientSupportC2S {
        registeredSerializers = Set.copyOf(registeredSerializers);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
