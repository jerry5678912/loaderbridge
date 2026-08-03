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
    "forge-runtime",
    "forge-transform-service",
    "integration-harness",
    "cli",
    "fixture-fabric-main",
    "fixture-fabric-api-base",
    "fixture-fabric-command",
    "fixture-fabric-lifecycle",
)
