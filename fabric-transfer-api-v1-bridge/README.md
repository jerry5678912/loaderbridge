# Fabric Transfer API v1 bridge

This module targets `fabric-transfer-api-v1` 5.4.4+7b3d111d19 for Minecraft
1.21.1. Revision 3 implements the public transaction and snapshot-participant
contracts: nested atomic scopes, rollback, commit propagation, callback order,
outer-close notifications, lifecycle inspection, and thread confinement. It
also implements the generic `Storage` and `StorageView` contracts, including
empty storage, capability flags, filtered iteration, and version guards. Common
composition types include slotted/single-slot views, ordered combined storages,
insertion/extraction restrictions, filtering wrappers, transfer variants,
resource amounts, and transfer preconditions.

Single-variant/NBT helpers, storage utilities, item, fluid, container-context,
inventory, and rendering surfaces remain gated until their behavioral fixtures pass. The
provider only advertises classes implemented by this revision.
