# Fabric Transfer API v1 controlled fixture

This fixture tests the item-provider increment of pinned
`fabric-transfer-api-v1:5.4.4+7b3d111d19` through LoaderBridge revision 8 on
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

The runtime marker was:

`LOADERBRIDGE_FABRIC_ITEM_PROVIDED_STORAGE_READY amount=500`

Prepared artifact evidence:

- bridge SHA-256: `f23e5a28bbb5cde1047cdae0124b45d50a04b620492afbfa83d8f6a620965bbc`
- fixture SHA-256: `b1684d811058ab7a37bffbd68b6f1a4f8aeb96de2a32683c9a736b439d81dc2f`
- lock SHA-256: `46be1e94aa2e0dea329e06445396226c855614fd03bc2b1a306bc2a6e581c968`

The public-surface contract test requires the exact advertised class and the
full repository build keeps unsupported FluidStorage references diagnostic-gated.
Built-in bundle/container-component providers, fluid storage, and client fluid
rendering remain open. This evidence is one Transfer API increment; it does not
complete the module, M5, or the 60% catalog gate.
