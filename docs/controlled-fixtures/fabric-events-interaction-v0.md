# Fabric Events Interaction v0 controlled fixture

This controlled Fabric-only mod tests the server/shared subset of pinned
`fabric-events-interaction-v0:0.7.14+ba9dae0619` through LoaderBridge on
Minecraft 1.21.1 and Forge 52.1.16.

The ordinary `prepare` command inspected class references and automatically
selected the interaction, API-base, and lifecycle bridges. No artifact-name or
mod-ID compatibility rule selected them.

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

Repeated preparation produced byte-identical artifacts:

- API-base bridge SHA-256: `338872b4c7690bc9c784275502f0b13b094db29a46fc0f881d141c79d01b2e32`
- interaction bridge SHA-256: `99adcac7928045d14c1f3f3e3fbd0104f988b9b69726f1be6e7e1b38ff85f642`
- lifecycle bridge SHA-256: `4c60518155e5402b78a764c2804ea8fe01e530c0a05ba6e6357c00a97ae472fa`
- transformed fixture SHA-256: `aea34e4a3da8b015bc3041d5dde823d2c07605162f1685b3ec220088b0712be4`

Lock files intentionally record absolute source and output locations, so locks
prepared in different directories differ only in those paths while retaining
the same artifact and cache hashes.

This closes the pinned server/shared interaction-events increment. Client pick
callbacks, client block-breaking callbacks, client pre-attack, fake-player,
and pick-aware interfaces remain open, so it does not close the entire Fabric
module, M5, or the 60% catalog gate.
