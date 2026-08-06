# Fabric Crash Report Info v1 bridge

This module independently implements the behavior of
`fabric-crash-report-info-v1:0.2.29+0af3f5a719` from Fabric API
`0.116.15+1.21.1`.

It adds translated Fabric root and nested mod IDs, names, and versions to
Minecraft's `SystemReport`. Root and child lists are sorted by mod ID, matching
the pinned Fabric contract. It exposes no public Java API; LoaderBridge selects
it from the inspected metadata dependency and installs its Mixin before
Minecraft constructs a system report.
