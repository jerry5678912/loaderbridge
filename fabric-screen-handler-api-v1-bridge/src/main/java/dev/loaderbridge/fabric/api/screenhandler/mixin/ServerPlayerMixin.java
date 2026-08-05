package dev.loaderbridge.fabric.api.screenhandler.mixin;

import dev.loaderbridge.fabric.api.screenhandler.ScreenHandlerNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    @Shadow public int containerCounter;

    private ServerPlayerMixin(net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos position, float yaw,
            com.mojang.authlib.GameProfile profile) {
        super(level, position, yaw, profile);
    }

    @Shadow public abstract void closeContainer();

    @Redirect(method = "openMenu", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;closeContainer()V"))
    private void loaderbridge$closeCurrentMenuIfAllowed(ServerPlayer player,
            MenuProvider provider) {
        if (((FabricScreenHandlerFactory) provider).shouldCloseCurrentScreen()) {
            closeContainer();
        } else {
            doCloseContainer();
        }
    }

    @Inject(method = "openMenu", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"
                    + "send(Lnet/minecraft/network/protocol/Packet;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void loaderbridge$storeOpenedMenu(MenuProvider provider,
            CallbackInfoReturnable<java.util.OptionalInt> callback, AbstractContainerMenu menu) {
        MenuProvider unwrapped = unwrap(provider);
        if (unwrapped instanceof ExtendedScreenHandlerFactory<?>) {
            containerMenu = menu;
        } else if (menu.getType() instanceof ExtendedScreenHandlerType<?, ?>) {
            throw new IllegalArgumentException("[Fabric] Extended screen handler "
                    + BuiltInRegistries.MENU.getKey(menu.getType())
                    + " must be opened with an ExtendedScreenHandlerFactory!");
        }
    }

    @Redirect(method = "openMenu", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"
                    + "send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void loaderbridge$replaceVanillaOpenPacket(ServerGamePacketListenerImpl listener,
            Packet<?> packet, MenuProvider provider) {
        MenuProvider unwrapped = unwrap(provider);
        if (unwrapped instanceof ExtendedScreenHandlerFactory<?> extendedFactory) {
            AbstractContainerMenu menu = java.util.Objects.requireNonNull(containerMenu);
            if (menu.getType() instanceof ExtendedScreenHandlerType<?, ?>) {
                send((ServerPlayer) (Object) this, extendedFactory, menu);
            } else {
                throw new IllegalArgumentException("[Fabric] Non-extended screen handler "
                        + BuiltInRegistries.MENU.getKey(menu.getType())
                        + " must not be opened with an ExtendedScreenHandlerFactory!");
            }
        } else {
            listener.send(packet);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void send(ServerPlayer player, ExtendedScreenHandlerFactory<?> provider,
            AbstractContainerMenu menu) {
        ScreenHandlerNetworking.sendOpenPacket(player,
                (ExtendedScreenHandlerFactory) provider, menu, containerCounter);
    }

    private static MenuProvider unwrap(MenuProvider provider) {
        if (provider instanceof SimpleMenuProvider simple) {
            var constructor = ((SimpleMenuProviderAccessor) (Object) simple)
                    .loaderbridge$getMenuConstructor();
            if (constructor instanceof ExtendedScreenHandlerFactory<?> extended) {
                return extended;
            }
        }
        return provider;
    }
}
