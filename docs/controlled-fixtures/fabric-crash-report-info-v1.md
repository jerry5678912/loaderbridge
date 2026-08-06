# Fabric Crash Report Info v1 controlled gate

Pinned contract: `fabric-crash-report-info-v1:0.2.29+0af3f5a719` from Fabric API
`0.116.15+1.21.1`.

The controlled Fabric-only fixture declares the module as a metadata dependency
without referencing a public API class. Preparation must therefore select the
bridge from inspected metadata, not a filename or binary symbol. During its
Fabric `main` entrypoint, the fixture creates Minecraft's real `SystemReport`
and fails unless the generated `Fabric Mods` section contains its translated
Fabric ID, display name, and version.

On 2026-08-06, that assertion passed independently on Forge 52.1.0 and Forge
52.1.16. Both builds created a fresh world, reached dedicated-server ready,
saved the Overworld, Nether, and End, stopped cleanly, restarted the process,
repeated the exact report assertion against the saved world, saved again, and
stopped cleanly.

This closes the pinned Crash Report Info module gate. It does not complete M5
or establish the 60% catalog acceptance gate.
