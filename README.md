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
- A tested TinyRemapper engine accepting Tiny v2 namespace mappings.
- Deterministic unsigned output JARs with generated `mods.toml` and
  `loaderbridge.json`, SHA-256 caching, `bridge.lock.json`, and
  `compatibility-report.json`.
- Recursive nested-JAR inspection (recursive transformation/loading is gated).
- A ServiceLoader-discovered `BridgeAdapter`, CLI commands, Forge language and
  transformation service registrations, custom Forge container scaffold, and a
  small independently implemented Fabric Loader API shim.

## Intentionally gated

The adapter currently rejects mods that require Minecraft namespace remapping,
mixins, access wideners, Fabric API, custom language adapters, or native-library
review. The low-level class remapper is present and golden-tested, but the
official Minecraft classpath/mapping resolver and resource transformations must
be connected before enabling it for ordinary mods. Forge launch verification,
client/server lifecycle entrypoints, patch packs, and real-mod probes are also
not implemented yet. These gaps produce stable diagnostics instead of a JAR
that is falsely labeled compatible.

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
```

Exit codes are `0` success, `2` invalid input, `3` unsupported capability or
dependency, `4` transformation failure, and `5` launch/verification failure.

The original mod JAR is never modified. Do not redistribute transformed
third-party artifacts unless their licenses permit it.
