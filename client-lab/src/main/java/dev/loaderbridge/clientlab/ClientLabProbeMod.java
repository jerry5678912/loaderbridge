package dev.loaderbridge.clientlab;

import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.client.gui.LoadingErrorScreen;

@Mod("loaderbridge_client_probe")
public final class ClientLabProbeMod {
    private static final String WORLD_ID = System.getProperty(
            "loaderbridge.probe.world", "loaderbridge-m1-world");
    private static final String CONTENT_BLOCK_ID = System.getProperty("loaderbridge.probe.block", "");

    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.WAITING_FOR_TITLE);
    private final AtomicReference<String> lastScreen = new AtomicReference<>();
    private final AtomicReference<BackupConfirmScreen> handledBackupScreen = new AtomicReference<>();
    private final AtomicBoolean reportedLoadWarnings = new AtomicBoolean();
    private BlockPos contentPosition;
    private Block contentBlock;
    private final ScheduledExecutorService readinessPoller = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "loaderbridge-client-ready-probe");
        thread.setDaemon(true);
        return thread;
    });

    public ClientLabProbeMod() {
        System.out.println("LOADERBRIDGE_CLIENT_PROBE_CONSTRUCTED");
        readinessPoller.scheduleAtFixedRate(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> advance(minecraft));
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    private void advance(Minecraft minecraft) {
        Object screen = minecraft.screen;
        String screenName = screen == null ? "<none>" : screen.getClass().getName();
        String previous = lastScreen.getAndSet(screenName);
        if (!screenName.equals(previous)) {
            System.out.println("LOADERBRIDGE_CLIENT_SCREEN=" + screenName);
        }

        if (screen instanceof AccessibilityOnboardingScreen) {
            minecraft.setScreen(new TitleScreen());
            return;
        }

        if (screen instanceof BackupConfirmScreen backupConfirm
                && (phase.get() == Phase.OPENING_FIRST || phase.get() == Phase.OPENING_RELOAD)) {
            if (handledBackupScreen.getAndSet(backupConfirm) == backupConfirm) return;
            var skipLabel = Component.translatable("selectWorld.backupJoinSkipButton");
            var skipButton = backupConfirm.children().stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .filter(button -> button.getMessage().equals(skipLabel))
                    .findFirst()
                    .orElse(null);
            if (skipButton == null) {
                fail(minecraft, "backup confirmation has no skip button");
                return;
            }
            skipButton.onPress();
            return;
        }

        Phase current = phase.get();
        if (screen instanceof PauseScreen
                && (current == Phase.OPENING_FIRST || current == Phase.OPENING_RELOAD)) {
            minecraft.setScreen(null);
            return;
        }

        if (screen instanceof LoadingErrorScreen && reportedLoadWarnings.compareAndSet(false, true)) {
            ModLoader.get().getWarnings().forEach(warning ->
                    System.out.println("LOADERBRIDGE_CLIENT_LOAD_WARNING=" + warning.formatToString()));
        }

        if (current == Phase.WAITING_FOR_TITLE && screen instanceof TitleScreen) {
            System.out.println("LOADERBRIDGE_CLIENT_TITLE_READY");
            if (phase.compareAndSet(current, Phase.OPENING_FIRST)) {
                openOrCreateWorld(minecraft);
            }
            return;
        }

        if (current == Phase.OPENING_FIRST
                && minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null
                && minecraft.getSingleplayerServer() != null
                && minecraft.getSingleplayerServer().isReady()) {
            if (phase.compareAndSet(current, Phase.SAVING_FIRST)) {
                System.out.println("LOADERBRIDGE_CLIENT_WORLD_READY");
                var server = minecraft.getSingleplayerServer();
                server.execute(() -> {
                    if (!prepareContentProbe(minecraft)) return;
                    server.saveEverything(false, true, true);
                    System.out.println("LOADERBRIDGE_CLIENT_WORLD_SAVED");
                    server.halt(false);
                    minecraft.execute(() -> {
                        minecraft.disconnect(new TitleScreen());
                        phase.set(Phase.WAITING_FOR_RELOAD_TITLE);
                    });
                });
            }
            return;
        }

        if (current == Phase.WAITING_FOR_RELOAD_TITLE
                && screen instanceof TitleScreen
                && minecraft.getSingleplayerServer() == null) {
            if (phase.compareAndSet(current, Phase.OPENING_RELOAD)) {
                minecraft.createWorldOpenFlows().openWorld(WORLD_ID, () -> fail(minecraft, "world reload cancelled"));
            }
            return;
        }

        if (current == Phase.OPENING_RELOAD
                && minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null
                && minecraft.getSingleplayerServer() != null
                && minecraft.getSingleplayerServer().isReady()) {
            if (phase.compareAndSet(current, Phase.COMPLETE)) {
                var server = minecraft.getSingleplayerServer();
                server.execute(() -> {
                    if (!verifyContentProbe(minecraft)) return;
                    readinessPoller.shutdownNow();
                    System.out.println("LOADERBRIDGE_CLIENT_WORLD_RELOADED");
                    System.out.println("LOADERBRIDGE_CLIENT_STOPPED");
                    minecraft.stop();
                });
            }
        }
    }

    private boolean prepareContentProbe(Minecraft minecraft) {
        if (CONTENT_BLOCK_ID.isBlank()) return true;
        ResourceLocation id = ResourceLocation.tryParse(CONTENT_BLOCK_ID);
        var block = id == null ? java.util.Optional.<Block>empty()
                : BuiltInRegistries.BLOCK.getOptional(id);
        if (block.isEmpty() || block.get().asItem() == net.minecraft.world.item.Items.AIR) {
            fail(minecraft, "content block or item is not registered: " + CONTENT_BLOCK_ID);
            return false;
        }
        var server = minecraft.getSingleplayerServer();
        var clientPlayer = minecraft.player;
        if (server == null || clientPlayer == null) {
            fail(minecraft, "content probe lost its integrated server or client player");
            return false;
        }
        var player = server.getPlayerList().getPlayer(clientPlayer.getUUID());
        if (player == null) {
            fail(minecraft, "content probe could not resolve the server player");
            return false;
        }
        contentBlock = block.get();
        contentPosition = player.blockPosition().offset(2, 0, 0);
        server.overworld().setBlockAndUpdate(contentPosition, contentBlock.defaultBlockState());
        if (!server.overworld().getBlockState(contentPosition).is(contentBlock)) {
            fail(minecraft, "could not place content block: " + CONTENT_BLOCK_ID);
            return false;
        }
        if (!player.getInventory().add(new ItemStack(contentBlock))) {
            fail(minecraft, "could not add content item to inventory: " + CONTENT_BLOCK_ID);
            return false;
        }
        System.out.println("LOADERBRIDGE_CONTENT_REGISTRY_READY=" + CONTENT_BLOCK_ID);
        System.out.println("LOADERBRIDGE_CONTENT_BLOCK_PLACED=" + contentPosition.toShortString());
        return true;
    }

    private boolean verifyContentProbe(Minecraft minecraft) {
        if (CONTENT_BLOCK_ID.isBlank()) return true;
        var server = minecraft.getSingleplayerServer();
        var clientPlayer = minecraft.player;
        if (server == null || clientPlayer == null) {
            fail(minecraft, "content reload probe lost its integrated server or client player");
            return false;
        }
        if (contentPosition == null || contentBlock == null
                || !server.overworld().getBlockState(contentPosition).is(contentBlock)) {
            fail(minecraft, "content block did not survive save and reload: " + CONTENT_BLOCK_ID);
            return false;
        }
        var player = server.getPlayerList().getPlayer(clientPlayer.getUUID());
        if (player == null || !player.getInventory().contains(new ItemStack(contentBlock))) {
            fail(minecraft, "content item did not survive inventory save and reload: " + CONTENT_BLOCK_ID);
            return false;
        }
        System.out.println("LOADERBRIDGE_CONTENT_BLOCK_RELOADED=" + CONTENT_BLOCK_ID);
        System.out.println("LOADERBRIDGE_CONTENT_ITEM_RELOADED=" + CONTENT_BLOCK_ID);
        return true;
    }

    private void openOrCreateWorld(Minecraft minecraft) {
        var levelData = minecraft.gameDirectory.toPath().resolve("saves").resolve(WORLD_ID).resolve("level.dat");
        if (Files.isRegularFile(levelData)) {
            minecraft.createWorldOpenFlows().openWorld(WORLD_ID, () -> fail(minecraft, "existing world open cancelled"));
            return;
        }

        var settings = new LevelSettings(
                "LoaderBridge M1 World",
                GameType.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                new GameRules(),
                WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(
                WORLD_ID,
                settings,
                WorldOptions.defaultWithRandomSeed(),
                WorldPresets::createNormalWorldDimensions,
                new TitleScreen());
    }

    private void fail(Minecraft minecraft, String message) {
        System.err.println("LOADERBRIDGE_CLIENT_FAILURE=" + message);
        readinessPoller.shutdownNow();
        minecraft.stop();
    }

    private enum Phase {
        WAITING_FOR_TITLE,
        OPENING_FIRST,
        SAVING_FIRST,
        WAITING_FOR_RELOAD_TITLE,
        OPENING_RELOAD,
        COMPLETE
    }
}
