package dev.loaderbridge.fabric.api.recipe.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientNegotiation;
import net.fabricmc.fabric.impl.recipe.ingredient.SupportedIngredientsConnection;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketEncoder.class)
public abstract class PacketEncoderMixin {
    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;"
                    + "Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/codec/StreamCodec;"
                            + "encode(Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void loaderbridge$captureSupportedSerializers(ChannelHandlerContext context,
            Packet<?> packet, ByteBuf buffer, CallbackInfo callback) {
        ChannelHandler handler = context.pipeline().get("packet_handler");
        if (handler instanceof SupportedIngredientsConnection connection) {
            CustomIngredientNegotiation.CURRENT_SUPPORTED_SERIALIZERS.set(
                    connection.loaderbridge$getSupportedCustomIngredients());
        }
    }

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;"
                    + "Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V",
            at = {
                    @At(value = "INVOKE",
                            target = "Lnet/minecraft/network/codec/StreamCodec;"
                                    + "encode(Ljava/lang/Object;Ljava/lang/Object;)V",
                            shift = At.Shift.AFTER, by = 1),
                    @At(value = "INVOKE",
                            target = "Lnet/minecraft/network/protocol/Packet;isSkippable()Z")
            })
    private void loaderbridge$releaseSupportedSerializers(ChannelHandlerContext context,
            Packet<?> packet, ByteBuf buffer, CallbackInfo callback) {
        CustomIngredientNegotiation.CURRENT_SUPPORTED_SERIALIZERS.remove();
    }
}
