# Changelog

## 0.1.0-SNAPSHOT

- Added launcher-neutral adapter contracts and stable structured diagnostics.
- Added safe Fabric metadata/nested-JAR inspection and local dependency planning.
- Added deterministic preparation, hashing, locks, reports, ASM analysis, and a
  golden-tested TinyRemapper wrapper.
- Added CLI inspect/prepare/verify commands with ServiceLoader adapter discovery.
- Added Forge language/transformation service scaffolds and the initial Loader
  API shim.

All notable user-facing changes will be recorded here.

## Unreleased

### Added

- Initial LoaderBridge project scaffold.
- Added validated, launcher-neutral repository provider, artifact, dependency,
  hashing, and pagination contracts for compatibility catalog providers.
- Added a ServiceLoader-discovered Modrinth v2 provider with ranked Fabric
  search, version/dependency parsing, and checksum-addressed downloads.
- Added an authenticated CurseForge provider with Fabric 1.21.1 discovery,
  paginated file resolution, dependency mapping, and verified CDN downloads.
- Added deterministic catalog freezing with platform-local ranking, latest-release
  selection, hash/source deduplication, top-up enforcement, and canonical JSON.
- Added paginated multi-provider catalog collection and the credential-aware
  `catalog freeze` CLI command.
