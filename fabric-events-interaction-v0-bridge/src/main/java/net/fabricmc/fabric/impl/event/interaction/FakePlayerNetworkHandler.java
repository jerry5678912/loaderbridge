package net.fabricmc.fabric.impl.event.interaction;

import net.fabricmc.fabric.impl.networking.UntrackedNetworkHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;

/** A no-output play handler for Fabric fake players. */
public final class FakePlayerNetworkHandler extends ServerGamePacketListenerImpl
        implements UntrackedNetworkHandler {
    private static final Connection FAKE_CONNECTION = new FakeConnection();

    public FakePlayerNetworkHandler(ServerPlayer player) {
        super(player.getServer(), FAKE_CONNECTION, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener callbacks) {
        if (callbacks != null) callbacks.onSuccess();
    }

    private static final class FakeConnection extends Connection {
        private FakeConnection() {
            super(PacketFlow.SERVERBOUND);
        }
    }
}
