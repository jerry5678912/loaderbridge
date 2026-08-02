# LoaderBridge

LoaderBridge is an Apache-2.0 Java 21 command-line scaffold for a directional
Fabric 1.21.1 to Forge 52.1.x compatibility engine. It is not a launcher and it
does not claim that arbitrary Fabric mods run on Forge yet.

## What works now

- Content-based `fabric.mod.json` discovery, including recursively declared JARs.
- Bounded archive reads, ZIP-path and duplicate rejection, and no preprocessing
  classloading.
- Schema-v1 metadata, entrypoint, environment, dependency, alias, conflict,
  mixin, access-widener, nested-JAR, and language-adapter inspection.
- Local Fabric-style dependency diagnostics and common extended-semver ranges.
- ASM reference inventory for Loader API, Fabric API, Minecraft, reflection-like
  strings, and native libraries.
- Checksum-verified Mojang 1.21.1 client artifacts plus bundled Fabric
  intermediary mappings, composed into an intermediary-to-official remap.
- A real TinyRemapper pipeline for classes, fields, methods, descriptors, and
  lambdas, tested against the Minecraft 1.21.1 client JAR.
- Deterministic unsigned output JARs with generated `mods.toml` and
  `loaderbridge.json`, SHA-256 caching, `bridge.lock.json`, and
  `compatibility-report.json`.
- Recursive nested-JAR inspection (recursive transformation/loading is gated).
- A ServiceLoader-discovered `BridgeAdapter`, a Forge language provider, a
  separate early transformation service, custom Forge mod container, and a
  small independently implemented Fabric Loader API shim.
- Native execution of ordinary Fabric `main` and dedicated `server`
  entrypoints on Forge, with `client` dispatch wired to Forge's client setup
  phase.
- A server verification harness that launches Forge, waits for ready, requests
  shutdown, and requires a clean world-save marker.
- ServiceLoader-discovered Modrinth and authenticated CurseForge providers with
  ranked search, release/dependency resolution, checksums, and verified caches.
- Deterministic catalog snapshots with platform quotas, cross-site hash/source
  deduplication, top-up enforcement, and canonical JSON serialization.
- Recursive required-dependency graph resolution, including Modrinth's
  version-only pins, deterministic installation ordering, and cycle detection.

## Intentionally gated

The adapter currently rejects mods that require mixins, access wideners, Fabric
API, custom language adapters, recursively loaded nested JARs, Loader API calls
outside the current shim, or native-library review. Member-style entrypoints,
patch packs, client verification, and real-mod probes are not yet implemented.
These gaps produce stable diagnostics instead of a JAR that is falsely labeled
compatible.

The current controlled server fixture has passed on Minecraft 1.21.1 with Forge
52.1.0: Fabric `main` ran, Fabric `server` ran during sided setup, Forge reached
ready, the existing world reloaded, and all dimensions saved during a clean
shutdown. This is a scaffold milestone, not evidence that arbitrary Fabric mods
are compatible.

## Build and use

```shell
./gradlew build
./gradlew :cli:installDist

cli/build/install/cli/bin/cli inspect path/to/mod.jar --json
cli/build/install/cli/bin/cli prepare \
  --minecraft 1.21.1 \
  --host forge \
  --forge-version recommended \
  --mods path/to/mods \
  --output path/to/prepared

cli/build/install/cli/bin/cli verify \
  --instance path/to/installed-forge-server \
  --side server \
  --timeout-seconds 120 \
  --expect-marker LOADERBRIDGE_FIXTURE_MAIN_READY \
  --expect-marker LOADERBRIDGE_FIXTURE_SERVER_READY

CURSEFORGE_API_KEY=... cli/build/install/cli/bin/cli catalog freeze \
  --snapshot-id 2026-08 \
  --frozen-at 2026-08-01T00:00:00Z \
  --output catalog-2026-08.json

cli/build/install/cli/bin/cli resolve \
  --project modrinth:AABBCCDD \
  --output path/to/resolved-instance
```

`catalog freeze` queries both official repositories. The CurseForge key is read
only from the process environment, is sent only to CurseForge API metadata
endpoints, and is never written to snapshots or sent to artifact CDNs.
`resolve` installs the selected release and its recursively required
dependencies under `mods/`, retaining verified downloads in `.cache/` and
writing `bridge.repository.lock.json`.

For verification, install the generated mod JAR, `forge-runtime`,
`forge-transform-service`, and `fabric-loader-shim` in the Forge instance's
`mods` directory first. `verify` uses that instance's `run.sh` or `run.bat` and
automatically uses the same Java runtime as LoaderBridge.

Exit codes are `0` success, `2` invalid input, `3` unsupported capability or
dependency, `4` transformation failure, and `5` launch/verification failure.

The original mod JAR is never modified. Do not redistribute transformed
third-party artifacts unless their licenses permit it.
