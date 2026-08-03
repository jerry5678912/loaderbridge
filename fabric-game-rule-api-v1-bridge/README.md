# Fabric Game Rule API v1 bridge

Targets `fabric-game-rule-api-v1:1.0.53+6ced4dd919` from Fabric API
`0.116.15+1.21.1`. It implements registration, built-in boolean/integer rules,
bounded integers, double rules, enum rules, validation, visitors, callbacks,
custom categories, serialization, copying, and duplicate detection against
Minecraft's native game-rule registry.
Because these public signatures contain Minecraft classes, the bridge is a
minimal Forge mod container so it is loaded in the transformed game layer;
packaging it as a plugin-layer library would create incompatible class identities.

Authoritative references:

- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-game-rule-api-v1/1.0.53%2B6ced4dd919/
- https://github.com/FabricMC/fabric-api/tree/0.116.15%2B1.21.1/fabric-game-rule-api-v1
