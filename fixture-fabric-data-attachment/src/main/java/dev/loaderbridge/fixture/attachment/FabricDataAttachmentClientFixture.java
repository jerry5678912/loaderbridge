package dev.loaderbridge.fixture.attachment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class FabricDataAttachmentClientFixture implements ClientModInitializer {
    private LocalPlayer observedPlayer;
    private boolean reportedForPlayer;
    private int synchronizedSessions;
    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "loaderbridge-attachment-sync-fixture");
                thread.setDaemon(true);
                return thread;
            });

    @Override public void onInitializeClient() {
        poller.scheduleAtFixedRate(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> verify(client));
        }, 50, 50, TimeUnit.MILLISECONDS);
    }

    private void verify(Minecraft client) {
        if (client.level == null || client.player == null) return;
        if (observedPlayer != client.player) {
            observedPlayer = client.player;
            reportedForPlayer = false;
        }
        if (reportedForPlayer) return;
        Integer levelValue = ((AttachmentTarget) client.level)
                .getAttached(FabricDataAttachmentFixture.SYNCED_LEVEL);
        Integer playerValue = ((AttachmentTarget) client.player)
                .getAttached(FabricDataAttachmentFixture.SYNCED_PLAYER);
        if (Integer.valueOf(53).equals(levelValue) && Integer.valueOf(59).equals(playerValue)) {
            reportedForPlayer = true;
            synchronizedSessions++;
            System.out.println("LOADERBRIDGE_DATA_ATTACHMENT_CLIENT_SYNC_READY level=53 player=59 session="
                    + synchronizedSessions);
        }
    }
}
