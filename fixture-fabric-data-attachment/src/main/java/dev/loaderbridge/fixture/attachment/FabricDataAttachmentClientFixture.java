package dev.loaderbridge.fixture.attachment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class FabricDataAttachmentClientFixture implements ClientModInitializer {
    private LocalPlayer observedPlayer;
    private boolean reportedForPlayer;
    private int synchronizedSessions;
    private String lastObservedValues = "no client world";
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
        if (client.level == null || client.player == null) {
            if (observedPlayer != null) {
                if (!reportedForPlayer) {
                    var failure = new IllegalStateException(
                            "Data attachment target sync incomplete: " + lastObservedValues);
                    client.emergencySaveAndCrash(CrashReport.forThrowable(
                            failure, "LoaderBridge data attachment fixture"));
                    return;
                }
                observedPlayer = null;
                reportedForPlayer = false;
            }
            return;
        }
        if (observedPlayer != client.player) {
            observedPlayer = client.player;
            reportedForPlayer = false;
        }
        if (reportedForPlayer) return;
        Integer levelValue = ((AttachmentTarget) client.level)
                .getAttached(FabricDataAttachmentFixture.SYNCED_LEVEL);
        Integer playerValue = ((AttachmentTarget) client.player)
                .getAttached(FabricDataAttachmentFixture.SYNCED_PLAYER);
        var syncPos = client.level.getSharedSpawnPos().above(2);
        var blockEntity = client.level.getBlockEntity(syncPos);
        Integer blockEntityValue = blockEntity == null ? null
                : ((AttachmentTarget) blockEntity)
                        .getAttached(FabricDataAttachmentFixture.SYNCED_BLOCK_ENTITY);
        var chunk = client.level.getChunkSource().getChunkNow(syncPos.getX() >> 4,
                syncPos.getZ() >> 4);
        Integer chunkValue = chunk == null ? null
                : ((AttachmentTarget) chunk).getAttached(FabricDataAttachmentFixture.SYNCED_CHUNK);
        boolean entityReady = false;
        var entityStates = new StringBuilder();
        for (Entity entity : client.level.entitiesForRendering()) {
            Integer value = ((AttachmentTarget) entity)
                    .getAttached(FabricDataAttachmentFixture.SYNCED_ENTITY);
            if (!entityStates.isEmpty()) entityStates.append(',');
            entityStates.append(entity.getId()).append('=').append(value);
            if (Integer.valueOf(67).equals(value)) {
                entityReady = true;
                break;
            }
        }
        lastObservedValues = "level=" + levelValue + " player=" + playerValue
                + " entity=" + entityReady + " entities=[" + entityStates + "] block="
                + blockEntityValue
                + " chunk=" + chunkValue;
        if (Integer.valueOf(53).equals(levelValue) && Integer.valueOf(59).equals(playerValue)
                && Integer.valueOf(71).equals(blockEntityValue)
                && Integer.valueOf(73).equals(chunkValue) && entityReady) {
            reportedForPlayer = true;
            synchronizedSessions++;
            System.out.println("LOADERBRIDGE_DATA_ATTACHMENT_TARGET_SYNC_READY "
                    + "level=53 player=59 entity=67 block=71 chunk=73 session="
                    + synchronizedSessions);
        }
    }
}
