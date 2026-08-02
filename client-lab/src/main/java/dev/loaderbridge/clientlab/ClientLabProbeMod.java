package dev.loaderbridge.clientlab;

import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.client.gui.LoadingErrorScreen;

@Mod("loaderbridge_client_probe")
public final class ClientLabProbeMod {
    private static final String WORLD_ID = "loaderbridge-m1-world";

    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.WAITING_FOR_TITLE);
    private final AtomicReference<String> lastScreen = new AtomicReference<>();
    private final AtomicBoolean reportedLoadWarnings = new AtomicBoolean();
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

        if (screen instanceof LoadingErrorScreen && reportedLoadWarnings.compareAndSet(false, true)) {
            ModLoader.get().getWarnings().forEach(warning ->
                    System.out.println("LOADERBRIDGE_CLIENT_LOAD_WARNING=" + warning.formatToString()));
        }

        Phase current = phase.get();
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
                readinessPoller.shutdownNow();
                System.out.println("LOADERBRIDGE_CLIENT_WORLD_RELOADED");
                System.out.println("LOADERBRIDGE_CLIENT_STOPPED");
                minecraft.stop();
            }
        }
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
