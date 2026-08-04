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
bed and requires all eight exercised sleep callback families: allow sleeping,
bed validity, sleep time, nearby monsters, setting spawn, bed occupation,
sleep direction, and wake position. It verifies start/stop sleeping, performs
the scenario once in a fresh world and again after reopening the saved world,
observes two player joins, then observes player leave on the final actual
disconnect. The client reached title and world readiness, saved, reloaded, and
exited with a successful Gradle run.

The implementation uses Forge events where their timing and values match and
early Mixins where they do not. In particular, death/combat callbacks wrap the
vanilla operations instead of firing from Forge's cancellable pre-death event,
`Mob.convertTo` is intercepted before spawn, and `ALLOW_BED` participates in
both initial occupation and wake-up paths.

Pinned inputs:

- Minecraft `1.21.1`
- Forge `52.1.16`
- Fabric Entity Events v1 `1.8.0+2b27e0a419`
- Java `21`
- macOS graphical client and dedicated-server laboratories

Prepared client artifact evidence:

- bridge SHA-256: `351c75393527b02b9f999e6c4ec72ebe4aed1b2a9181f0dde758099096189ed5`
- fixture SHA-256: `f1b44e33b2d381a239c1aaa430f036b2a7b9d79c376df0a8c4654b0a04546851`
- lock SHA-256: `8dc100607ce03793ca279c9b716768fbe6e2961bfddc2cef969222e760ac1243`

Prepared server artifact evidence:

- bridge SHA-256: `351c75393527b02b9f999e6c4ec72ebe4aed1b2a9181f0dde758099096189ed5`
- fixture SHA-256: `f1b44e33b2d381a239c1aaa430f036b2a7b9d79c376df0a8c4654b0a04546851`
- lock SHA-256: `17f5b97f2097a2aaac74f00e50a856b047eb091ee0e56fe1b7f01ee2f5ce40ab`

This closes the controlled damage, death, conversion, player connection, and
sleep wave of the Entity Events v1 M5 increment. Elytra, respawn/copy, and
cross-dimension behavior still require equally deep runtime scenarios before
the whole module is declared behaviorally complete. It does not complete M5,
prove Windows/Linux parity, or establish the 60% catalog gate.
