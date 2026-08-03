# Fabric Transfer API v1 bridge

This module targets `fabric-transfer-api-v1` 5.4.4+7b3d111d19 for Minecraft
1.21.1. Revision 1 implements the public transaction and snapshot-participant
contracts: nested atomic scopes, rollback, commit propagation, callback order,
outer-close notifications, lifecycle inspection, and thread confinement.

Generic storage, item, fluid, container-context, inventory, and rendering
surfaces remain gated until their behavioral fixtures pass. The provider only
advertises the transaction classes implemented by this revision.
