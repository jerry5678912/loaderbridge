# Fabric Entity Events v1 controlled fixture

This fixture tests the pinned `fabric-entity-events-v1:1.8.0+2b27e0a419`
contract through LoaderBridge on Forge 52.1.16. The source JAR contains only
Fabric-facing metadata and API calls. Inspection of both outer API types and
nested callback descriptors automatically installs Entity Events, Lifecycle
Events, and API Base bridge modules; it does not select modules by filename.

The dedicated-server scenario creates real Minecraft entities and proves:

- damage permission and post-damage callbacks, including base and taken values;
- exact fatal-damage amount propagation and death cancellation;
- actual post-death and post-combat-kill callbacks at their Fabric lifecycle points;
- zombie-to-drowned conversion before the converted mob is spawned, including
  the `keepEquipment` argument;
- clean world flush, shutdown, second-process reload, and another clean stop.

The graphical integrated-server scenario additionally creates a real two-block
bed and requires all nine sleep callback families: allow sleeping, bed
validity, sleep time, nearby monsters, resetting-time veto, setting spawn, bed
occupation, sleep direction, and wake position. It verifies start/stop
sleeping, custom elytra start and tick, a mob replacement from the Overworld
to the Nether, a player Nether round trip, and an actual alive player respawn.
The respawn must deliver the same old/new pair to copy and after-respawn
callbacks, preserve the UUID and experience level, and propagate `alive=true`.
The complete scenario runs once in a fresh world and again after reopening the
saved world, observes two player joins, then observes player leave on the final
actual disconnect. The client-specific custom-elytra mixin also applies during
graphical startup. The client reached title and world readiness, saved,
reloaded, and exited with a successful Gradle run.

The implementation uses Forge events where their timing and values match and
early Mixins where they do not. In particular, death/combat callbacks wrap the
vanilla operations instead of firing from Forge's cancellable pre-death event,
`Mob.convertTo` is intercepted before spawn, `ALLOW_BED` participates in both
initial occupation and wake-up paths, and queued fixture assertion failures are
re-thrown from the server tick so they cannot be hidden as successful launches.

Pinned inputs:

- Minecraft `1.21.1`
- Forge `52.1.16`
- Fabric Entity Events v1 `1.8.0+2b27e0a419`
- Java `21`
- macOS graphical client and dedicated-server laboratories

Prepared client artifact evidence:

- bridge SHA-256: `a888d3e19f02b35a9b6daa5f5a7314bb5a685e7bc0be31c103d50c662a90b477`
- fixture SHA-256: `ebf6fe5c411e61137f638a3ebb47a84d995eb2c0c58de21e950fbc7fba3469a8`
- lock SHA-256: `7e20eedfc187b5f4dc75b2dde5cd6c6d82afb7f40f53497913ab9cabe41aaac4`

Prepared server artifact evidence:

- bridge SHA-256: `a888d3e19f02b35a9b6daa5f5a7314bb5a685e7bc0be31c103d50c662a90b477`
- fixture SHA-256: `f47602e58a7c7441da6ea4c1cd666f12a00c7dd24d643e5bfcb5ad400c15781f`
- lock SHA-256: `f8f2b1b75650ee791f182135da6c682042e3e3d7721eca914ecd14b2749bae0a`

This closes the controlled Entity Events v1 M5 module gate for damage, death,
conversion, player connection, sleep, elytra, respawn/copy, and entity/player
cross-dimension behavior. It does not complete M5, prove Windows/Linux parity,
exercise a physical client jump input for custom elytra, or establish the 60%
catalog gate.
