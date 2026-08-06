# Fabric Dimensions v1 controlled gate

Pinned contract: `fabric-dimensions-v1:4.0.1+65213ef819` from Fabric API
`0.116.15+1.21.1`.

Fabric allows a saved world to reopen after a custom dimension's datapack or
mod is removed. Forge 52.1.x already replaces the vanilla dimension map codec
with a lenient codec, but its decode still returns a partial error. Minecraft's
world-data `getOrThrow` rejects that result. LoaderBridge therefore replaces
that map boundary with a success-producing fail-soft codec that retains valid
dimensions and drops invalid entries.

DataFixerUpper must also preserve unknown dimension choices before Forge creates
its secure module layers. The adapter automatically emits a small isolated
startup agent when the Dimensions bridge is selected. The agent patches only
the two pinned DFU 8.0.16 tagged-choice classes, while the normal ModLauncher
transformer marks only Minecraft's two V2832 dimension choices as fail-soft.
The agent contains relocated ASM and activates only when a bridge JAR declares
`LoaderBridge-Dimensions-DataFix: true`.

Automatic selection is based on inspected Fabric metadata or binary references,
not filenames or individual mod IDs. Preparation copies the agent to
`.loaderbridge/agents/dimensions-datafix-agent.jar` and records the required
`-javaagent` option in `loaderbridge.launch.json`.

On 2026-08-06, disposable dedicated servers passed the same deep scenario on
Forge 52.1.0 and Forge 52.1.16:

- loaded a controlled datapack defining `loaderbridge:removable`;
- created a new world and reached ready;
- force-loaded its first chunk and placed a diamond block in the custom
  dimension through Minecraft's real command path;
- flushed every dimension and stopped cleanly;
- moved the controlled datapack into a recoverable fixture directory;
- reopened the same world in a second JVM despite its obsolete saved dimension;
- confirmed `loaderbridge:removable` was absent from the live dimension set;
- saved the remaining Overworld, Nether, and End and stopped cleanly.

This closes the pinned Fabric Dimensions v1 module gate. It does not complete
M5 or the catalog-wide 60% behavioral score.
