package net.fabricmc.loader.api.entrypoint;

@FunctionalInterface
public interface PreLaunchEntrypoint {
    void onPreLaunch();
}
