# Fabric resource loader v0 bridge

This module targets the official
`fabric-resource-loader-v0:1.3.1+5b5275af19` contract shipped by Fabric API
`0.116.15+1.21.1`. The first M5 release implements the complete public binary
type surface and Forge-backed server-data reload listener registration,
including registry-aware factories, duplicate suppression, Fabric IDs, and
dependency ordering. Built-in pack discovery and client reload registration
remain gated work rather than being counted as behavioral compatibility.

Authoritative references:

- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-resource-loader-v0/1.3.1%2B5b5275af19/
- https://github.com/FabricMC/fabric-api/tree/0.116.15%2B1.21.1/fabric-resource-loader-v0
- https://github.com/MinecraftForge/MinecraftForge/blob/1.21.x/src/main/java/net/minecraftforge/event/AddReloadListenerEvent.java
