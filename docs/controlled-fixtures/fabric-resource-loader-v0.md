# Fabric Resource Loader v0 controlled fixture

This fixture tests the pinned
`fabric-resource-loader-v0:1.3.1+5b5275af19` contract through LoaderBridge
revision 2 on Minecraft 1.21.1 and Forge 52.1.16.

The ordinary `prepare` command inspected the translated Fabric fixture and
automatically selected Resource Loader v0 plus API Base. The fixture registers
`loaderbridge:builtin_fixture` with `DEFAULT_ENABLED`; the pack intentionally
omits `pack.mcmeta` to exercise Fabric-compatible metadata fallback. Its data
resource is then read through the live server resource manager.

The runtime marker was:

`LOADERBRIDGE_FABRIC_BUILTIN_PACK_READY default_enabled=true`

The dedicated-server gate passed in a fresh world and a second JVM loading the
saved world. Forge logged that it found and automatically loaded
`loaderbridge:builtin_fixture`; both processes reached ready, saved all three
dimensions, and stopped cleanly. The graphical gate observed the same marker
in both integrated-server sessions, saved and reopened the world, and stopped
cleanly.

Prepared artifact evidence:

- bridge SHA-256: `2fa10415db223a9fdb8858fbbe8be1d2285cd2bf449108ce5c824e3631f89fee`
- transformed fixture SHA-256: `fecc50bbd0dc1d18f8678e1b37a234cc183ff20925a47363eba260571f49c4c4`
- lock SHA-256: `2990818c55222174e284d01d800f7e9eb46d1dbef179c6954066bada460ddfc0`

Two independent preparations produced identical JAR digest lists. Unit
contracts additionally prove `NORMAL`, `DEFAULT_ENABLED`, and `ALWAYS_ENABLED`
selection mapping, Fabric's default display name, missing-pack rejection, and
canonical containment for pack roots, individual files, and overlay paths.

This closes the built-in-pack increment of Resource Loader v0. It does not
complete every Resource Loader surface, M5, or the roadmap's 60% catalog gate.
