# Fabric Transfer API v1 controlled fixture

This fixture tests the item-provider and server-safe fluid-storage surface of pinned
`fabric-transfer-api-v1:5.4.4+7b3d111d19` through LoaderBridge revision 11 on
Minecraft 1.21.1 and Forge 52.1.16.

The implementation follows Fabric's public `SingleVariantItemStorage`
contract: a fixed-capacity storage replaces the containing item variant inside
the caller's `ContainerItemContext`, allowing item components to carry the
resource and amount while the transaction controls rollback or commit.

The source fixture is compiled as a Fabric mod and transformed by the ordinary
`prepare` command. Content inspection automatically selected Transfer API,
API Lookup, and their required bridge dependencies without using filenames or
mod IDs.

The graphical Forge scenario proved:

- `ItemStorage.ITEM.registerForItems` exposes a provider for the fixture item;
- `ContainerItemContext.find(ItemStorage.ITEM)` returns that provider;
- an aborted insertion of 250 units leaves the container unchanged;
- a committed insertion stores 700 units in the item's custom-data component;
- a committed extraction removes 200 units and an insertion of a different
  resource is rejected;
- the final item remains the original container with 500 units stored and a
  reported capacity of 1,000;
- an aborted 70-diamond shulker insertion restores an empty component, while
  committed insertion and extraction leave 59 and 6 diamonds in the first two
  of 27 stable slots;
- an aborted 20-diamond bundle insertion restores empty contents, while
  committed insertion and extraction leave 15 diamonds under vanilla bundle
  weight rules;
- the process creates and saves a world, reopens it, repeats its networking and
  lifecycle gates, and stops cleanly.
- flowing water is normalized to a still-water `FluidVariant`;
- aborted fluid insertion/extraction rolls back and committed operations leave
  54,000 droplets in a 162,000-droplet `SingleFluidStorage`;
- an empty vanilla bucket fills with one bucket of water and drains back to an
  empty bucket through transactional item lookup;
- `FluidStorage.SIDED` discovers the registered world storage and its resource
  and amount survive an NBT round-trip;
- two dedicated-server launches independently pass fresh-world and saved-world
  readiness, save, and clean shutdown.
- a glass bottle accepts 27,000 droplets of water as a vanilla water potion and
  drains back into an empty glass bottle;
- vanilla water/lava attribute temperature and fill-sound contracts resolve;
- `FluidStorageUtil` fills and drains the survival player's hand in both
  graphical connection sessions;
- an aborted cauldron mutation restores the empty block, committed insert and
  extraction leave water level 1, and that level is observed after reload.
- composter lookup is vertical-side-only; top insertion and bottom bone-meal
  extraction each roll back when aborted and mutate the real block when
  committed in both graphical sessions and both dedicated-server processes.

The runtime marker was:

`LOADERBRIDGE_FABRIC_ITEM_PROVIDED_STORAGE_READY amount=500`

`LOADERBRIDGE_FABRIC_ITEM_BUILTIN_STORAGE_READY shulker=65,bundle=15`

`LOADERBRIDGE_FABRIC_COMPOSTER_STORAGE_READY insert=1,extract=1`

`LOADERBRIDGE_FABRIC_FLUID_STORAGE_READY amount=54000`

`LOADERBRIDGE_FABRIC_FLUID_SIDED_NBT_READY`

`LOADERBRIDGE_FABRIC_FLUID_POTION_ATTRIBUTES_READY`

`LOADERBRIDGE_FABRIC_FLUID_PLAYER_UTILITY_READY`

`LOADERBRIDGE_FABRIC_FLUID_CAULDRON_READY`

`LOADERBRIDGE_FABRIC_FLUID_CAULDRON_RELOADED`

Prepared artifact evidence:

- bridge SHA-256: `a6cf86a65ef754660323a80ce950f915d9bcc2447de62a8931cd460fbb25d267`
- fixture SHA-256: `fbd909693a787d4d3a0e782b6efdd107a175a81e608865b38383fb430d1e1c0a`

The public-surface contract test requires the exact advertised classes and the
full repository build keeps unsupported client fluid-rendering references
diagnostic-gated. This closes the pinned common/server Transfer module surface;
client fluid rendering remains for M6. It does not complete M5 or establish the
60% catalog gate.
