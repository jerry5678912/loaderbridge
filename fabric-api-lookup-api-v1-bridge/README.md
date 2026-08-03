# Fabric API Lookup API v1 bridge

This module implements the public lookup contracts from
`fabric-api-lookup-api-v1` 1.6.72+d30f6a7919 for Minecraft 1.21.1.

Implemented now:

- Unique, runtime-type-safe `BlockApiLookup` creation.
- Block, block-entity, self, and ordered fallback provider registration.
- Provider-first lookup behavior with live block-state and block-entity queries.
- `BlockApiCache` with correct live results and public metadata accessors.
- Item direct, self, and ordered fallback providers.
- Entity direct, self, and ordered fallback providers, including deferred
  validation once a server world is available.
- Ordered, runtime-type-safe custom `ApiLookupMap` instances.
- Copy-on-write, identity-keyed custom `ApiProviderMap` instances.

The cache currently prioritizes correctness over Fabric's invalidation-based
performance optimization.
