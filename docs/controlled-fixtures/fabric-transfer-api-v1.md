# Fabric Transfer API v1 controlled fixture

This fixture tests the item-provider and first fluid-storage increments of pinned
`fabric-transfer-api-v1:5.4.4+7b3d111d19` through LoaderBridge revision 9 on
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

The runtime marker was:

`LOADERBRIDGE_FABRIC_ITEM_PROVIDED_STORAGE_READY amount=500`

`LOADERBRIDGE_FABRIC_FLUID_STORAGE_READY amount=54000`

`LOADERBRIDGE_FABRIC_FLUID_SIDED_NBT_READY`

Prepared artifact evidence:

- bridge SHA-256: `2ece431397b6a8d7a98a9ddbb9925c9c71d903d38a2d31a40ad5ddd66bdc2ead`
- fixture SHA-256: `f83feb03ecc0480c9cbc0df6a9832c9826450d59ee018b36ed66f5c0d4aceda1`
- lock SHA-256: `d762d71b5a6b0dd52ac56d6e85f97da8012997724c96735e4017bbd2ecf9d30d`

The public-surface contract test requires the exact advertised classes and the
full repository build keeps unsupported fluid-attribute references
diagnostic-gated. Built-in potion/cauldron, bundle/container-component,
fluid-attribute, and client fluid-rendering surfaces remain open. This evidence
is one Transfer API increment; it does not complete the module, M5, or the 60%
catalog gate.
