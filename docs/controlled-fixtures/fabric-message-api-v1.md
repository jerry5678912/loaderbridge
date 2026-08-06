# Fabric Message API v1 server/shared controlled gate

Pinned contract: `fabric-message-api-v1:6.0.14+6ced4dd919` from Fabric API
`0.116.15+1.21.1`.

This increment implements the common/server public surface:

- content, default, and styling message-decorator phases;
- allow/cancel and observation callbacks for player chat;
- allow/cancel and observation callbacks for game/system messages;
- allow/cancel and observation callbacks for command messages.

The hooks run at Minecraft's `PlayerList` broadcast boundaries and replace the
server chat decorator through the same `MinecraftServer` return boundary used
by Fabric. Automatic selection uses inspected binary references or metadata and
installs API Base as its dependency.

The first real launch exposed a generic class-loader error: API Base was a Forge
plugin-layer library while Message API lived in the game layer, and both
resolved different `ResourceLocation` classes. API Base revision 2 is now an
ordinary game-layer Forge mod. That keeps all public Fabric events whose
signatures contain Minecraft types in the same layer as their consumers.

On 2026-08-06, the controlled Fabric-only fixture passed fresh-world and
saved-world launches independently on Forge 52.1.0 and Forge 52.1.16. Every run:

- decorated a message in exact content, default, then styling order;
- delivered one allowed system message and canceled one blocked message;
- invoked the command allow and notification callbacks through the real `/say`
  command path;
- reached ready, saved the Overworld, Nether, and End, and stopped cleanly.

Client send/receive events remain explicitly unprovided and produce a stable
`LB-FAPI-001` diagnostic when required. They are an M6 client milestone. This
closes the server/shared Message API increment, not the complete module, M5, or
the 60% catalog gate.
