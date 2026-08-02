# LoaderBridge client laboratory

This standalone ForgeGradle build is a test-only M2 client probe. It is kept out
of the main multi-module build so ordinary unit tests do not download or launch
Minecraft.

Run it with Java 21 from the repository root:

```shell
./gradlew -p client-lab runClient
```

The task launches Minecraft 1.21.1 with Forge 52.1.0. The probe handles the
first-run accessibility screen, reaches the real title screen, opens or creates
`loaderbridge-m1-world`, waits for the player and integrated server to be ready,
saves every dimension, cleanly disconnects, reloads the same world, and stops.
Success requires Gradle exit code `0`, the Fabric `preLaunch`, `main`, and
`client` markers, plus these lifecycle markers:

```text
LOADERBRIDGE_CLIENT_TITLE_READY
LOADERBRIDGE_CLIENT_WORLD_READY
LOADERBRIDGE_CLIENT_WORLD_SAVED
LOADERBRIDGE_CLIENT_WORLD_RELOADED
LOADERBRIDGE_CLIENT_STOPPED
```

The generated game directory, world, assets, logs, and Forge artifacts are local
build/cache data and are not committed or redistributed.
