# Fabric Events Interaction v0 controlled fixture

This controlled Fabric-only mod tests the server/shared subset of pinned
`fabric-events-interaction-v0:0.7.14+ba9dae0619` through LoaderBridge on
Minecraft 1.21.1 and Forge 52.1.16.

The ordinary `prepare` command inspected class references and automatically
selected the interaction, API-base, lifecycle, and networking bridges. The
networking module is a declared transitive requirement of the FakePlayer
surface. No artifact-name or mod-ID compatibility rule selected them.

In a real integrated world the fixture placed blocks, spawned an entity, and
called Minecraft's actual server interaction paths. The bridge observed one
block attack, one entity attack, one block use, one item use, one entity use,
two before-break callbacks, one canceled break, and one successful after-break
callback. It also proved Fabric's first-non-`PASS` result ordering and verified
that the canceled block remained while the successful block was removed. The
run emitted:

`LOADERBRIDGE_FABRIC_INTERACTION_EVENTS_READY attackBlock=1,attackEntity=1,useBlock=1,useItem=1,useEntity=1,before=2,canceled=1,after=1,cancelResult=false,breakResult=true,results=FAIL/FAIL/FAIL`

The same graphical Forge process reached the title screen, created a clean
world, emitted the behavioral marker, saved every dimension, reopened that
world, and stopped cleanly. Two dedicated-server processes independently
created and reloaded `loaderbridge-interaction-server-r1`; both emitted the
module load marker, reached `Done`, saved every dimension, and stopped cleanly.

Revision 13 adds a dedicated FakePlayer behavioral gate. On each integrated
and dedicated server start, the translated fixture verifies default and custom
profile caching, the pinned default UUID and name, profile separation, the
no-output connection, the untracked networking marker, invulnerability,
absence from the real player list, and no-op sleeping, riding, and menu
behavior. Both integrated-server launches and both dedicated-server processes
emitted:

`LOADERBRIDGE_FABRIC_FAKE_PLAYER_READY profile=[LoaderBridge Fixture],cached=true,untracked=true`

Repeated preparation produced byte-identical artifacts:

- API-base bridge SHA-256: `338872b4c7690bc9c784275502f0b13b094db29a46fc0f881d141c79d01b2e32`
- interaction bridge SHA-256: `4e227c27986d2f8bf58677763ea7bed7fc4167a9f996b57d5fa82091d4b82022`
- lifecycle bridge SHA-256: `4c60518155e5402b78a764c2804ea8fe01e530c0a05ba6e6357c00a97ae472fa`
- networking bridge SHA-256: `1305d2afbfdf695deeb20d1e854aef2395e1334a67b7a3823e4371088e8648b1`
- transformed fixture SHA-256: `0c4b8b90695be6e0cdabb7aa5804e4b60489f7e91a23fb2f6da5dcf74726ad0d`

Lock files intentionally record absolute source and output locations, so locks
prepared in different directories differ only in those paths while retaining
the same artifact and cache hashes.

This closes the pinned server/shared interaction-events and FakePlayer
increments. Client pick callbacks, client block-breaking callbacks, client
pre-attack, and pick-aware interfaces remain open, so it does not close the
entire Fabric module, M5, or the 60% catalog gate.
