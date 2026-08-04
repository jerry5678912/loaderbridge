pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        exclusiveContent {
            forRepository {
                maven("https://libraries.minecraft.net/")
            }
            filter {
                includeModule("com.mojang", "logging")
            }
        }
    }
}

rootProject.name = "loaderbridge"

include(
    "bridge-api",
    "repository-modrinth",
    "repository-curseforge",
    "compatibility-catalog",
    "scenario-api",
    "scenario-yaml",
    "fabric-metadata",
    "fabric-remap",
    "fabric-loader-shim",
    "fabric-api-base-bridge",
    "fabric-command-api-v2-bridge",
    "fabric-lifecycle-events-bridge",
    "fabric-resource-loader-v0-bridge",
    "fabric-resource-conditions-api-v1-bridge",
    "fabric-loot-api-v3-bridge",
    "fabric-recipe-api-v1-bridge",
    "fabric-game-rule-api-v1-bridge",
    "fabric-object-builder-api-v1-bridge",
    "fabric-api-lookup-api-v1-bridge",
    "fabric-registry-sync-v0-bridge",
    "fabric-networking-api-v1-bridge",
    "fabric-transfer-api-v1-bridge",
    "fabric-content-registries-v0-bridge",
    "fabric-item-group-api-v1-bridge",
    "fabric-convention-tags-v2-bridge",
    "fabric-biome-api-v1-bridge",
    "fabric-blockrenderlayer-v1-bridge",
    "fabric-rendering-v1-bridge",
    "forge-runtime",
    "forge-transform-service",
    "integration-harness",
    "cli",
    "fixture-fabric-main",
    "fixture-fabric-api-base",
    "fixture-fabric-command",
    "fixture-fabric-lifecycle",
    "fixture-fabric-loot",
    "fixture-fabric-recipe",
    "fixture-fabric-nested-child",
)
