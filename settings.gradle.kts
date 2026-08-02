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
    }
}

rootProject.name = "loaderbridge"

include(
    "bridge-api",
    "repository-modrinth",
    "repository-curseforge",
    "fabric-metadata",
    "fabric-remap",
    "fabric-loader-shim",
    "forge-runtime",
    "forge-transform-service",
    "integration-harness",
    "cli",
    "fixture-fabric-main",
)
