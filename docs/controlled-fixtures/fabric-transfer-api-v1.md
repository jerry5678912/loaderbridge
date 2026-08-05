# Fabric Transfer API v1 controlled fixture

This fixture tests the item-provider and server-safe fluid-storage surface of pinned
`fabric-transfer-api-v1:5.4.4+7b3d111d19` through LoaderBridge revision 10 on
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

The runtime marker was:

`LOADERBRIDGE_FABRIC_ITEM_PROVIDED_STORAGE_READY amount=500`

`LOADERBRIDGE_FABRIC_FLUID_STORAGE_READY amount=54000`

`LOADERBRIDGE_FABRIC_FLUID_SIDED_NBT_READY`

`LOADERBRIDGE_FABRIC_FLUID_POTION_ATTRIBUTES_READY`

`LOADERBRIDGE_FABRIC_FLUID_PLAYER_UTILITY_READY`

`LOADERBRIDGE_FABRIC_FLUID_CAULDRON_READY`

`LOADERBRIDGE_FABRIC_FLUID_CAULDRON_RELOADED`

Prepared artifact evidence:

- bridge SHA-256: `68258b19cd902b8f7df135028921efa5b82694d3185edbee8d8093d256f3cb5d`
- fixture SHA-256: `1fdee685abbdd6a767b9dfd3b7c65e8e9ed914085091d518307e2813cd09bab3`
- lock SHA-256: `1806880fc8dbe712ed2c3b218c291756187ae98fa91ed9972be909e8ab4e768c`

The public-surface contract test requires the exact advertised classes and the
full repository build keeps unsupported client fluid-rendering references
diagnostic-gated. Bundle/container-component and client fluid-rendering surfaces
remain open. This evidence is one Transfer API increment; it does not complete
the module, M5, or the 60% catalog gate.
