# Fabric API Lookup API v1 bridge

This module implements the block lookup contracts from
`fabric-api-lookup-api-v1` 1.6.72+d30f6a7919 for Minecraft 1.21.1.

Implemented now:

- Unique, runtime-type-safe `BlockApiLookup` creation.
- Block, block-entity, self, and ordered fallback provider registration.
- Provider-first lookup behavior with live block-state and block-entity queries.
- `BlockApiCache` with correct live results and public metadata accessors.

The cache currently prioritizes correctness over Fabric's invalidation-based
performance optimization. Item, entity, and custom lookup maps remain
unadvertised until their full contracts and runtime scenarios are implemented.
