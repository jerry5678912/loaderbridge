package net.fabricmc.fabric.impl.recipe.ingredient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CustomIngredientQueryS2C(int protocolVersion) implements CustomPacketPayload {
    public static final Type<CustomIngredientQueryS2C> TYPE =
            new Type<>(CustomIngredientNegotiation.CHANNEL_S2C);
    public static final StreamCodec<FriendlyByteBuf, CustomIngredientQueryS2C> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT,
                    CustomIngredientQueryS2C::protocolVersion, CustomIngredientQueryS2C::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
