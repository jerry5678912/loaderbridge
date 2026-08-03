# Fabric Command API v2 bridge

This module independently implements the server `CommandRegistrationCallback`
surface from `fabric-command-api-v2:2.2.28+6ced4dd919` and publishes Forge's
`RegisterCommandsEvent` with its dispatcher, build context, and command
selection unchanged. Other command-v2 classes remain explicitly gated.

Official references:

- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-command-api-v2/2.2.28%2B6ced4dd919/
- https://github.com/FabricMC/fabric-api/tree/0.116.15%2B1.21.1/fabric-command-api-v2
- https://github.com/MinecraftForge/MinecraftForge/blob/1.21.x/src/main/java/net/minecraftforge/event/RegisterCommandsEvent.java
