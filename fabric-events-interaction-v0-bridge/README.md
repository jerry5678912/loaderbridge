# Fabric Events Interaction v0 bridge

This module targets the server/shared portion of
`fabric-events-interaction-v0` 0.7.14+ba9dae0619 for Minecraft 1.21.1. It
provides the pinned contracts for:

- attacking blocks and entities;
- using blocks, entities, and items;
- block-break before, after, and canceled callbacks;
- Fabric's internal block-attack interaction hook.

Forge interaction events are translated at highest priority while retaining
Fabric's ordered, first-non-`PASS` callback semantics. A small early Mixin
captures successful server-side block destruction so `AFTER` runs only after
the vanilla operation succeeds. Canceled break state is synchronized back to
the player.

The runtime module is discovered through `ServiceLoader` and selected from
inspected binary API references, including the nested block-break callback
types. It does not advertise the still-unimplemented client pick, client block
break, client pre-attack, or fake-player surfaces; references to those APIs
therefore remain explicit `LB-FAPI-001` preparation failures.

Behavioral evidence is documented in
`docs/controlled-fixtures/fabric-events-interaction-v0.md`.
