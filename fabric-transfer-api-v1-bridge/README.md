# Fabric Transfer API v1 bridge

This module targets `fabric-transfer-api-v1` 5.4.4+7b3d111d19 for Minecraft
1.21.1. Revision 7 implements the public transaction and snapshot-participant
contracts: nested atomic scopes, rollback, commit propagation, callback order,
outer-close notifications, lifecycle inspection, and thread confinement. It
also implements the generic `Storage` and `StorageView` contracts, including
empty storage, capability flags, filtered iteration, and version guards. Common
composition types include slotted/single-slot views, ordered combined storages,
insertion/extraction restrictions, filtering wrappers, transfer variants,
resource amounts, and transfer preconditions.
`StorageUtil` covers atomic moves, simulations, extraction queries, stacking,
resource discovery, and comparator output. `SingleVariantStorage` and
`BlankVariantView` provide snapshot-backed single-resource state, NBT codecs,
and empty-capacity hints.
Item variants, transactional single-stack/item storage, and vanilla inventory
wrapping cover count splitting, component identity, sided access rules, slot
capacity, rollback, commit, and dirty notification.
`ItemStorage.SIDED` integrates those wrappers with Fabric's standard block API
lookup and automatically discovers vanilla container block entities. The
lookup bridge and its transitive dependencies are selected automatically.
Player inventory storage adds Fabric-compatible stacking order, transactional
overflow drops, main/offhand slot views, and cursor storage. Container item
contexts cover player, creative, cursor, single-slot, and immutable simulation
contexts, including transactional item exchange and overflow routing.

Item-provided storage registration, fluid storage, specialized single-variant
item storage, and rendering surfaces remain gated until their behavioral
fixtures pass. The provider only advertises classes implemented by this
revision.
