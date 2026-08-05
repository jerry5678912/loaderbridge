plugins {
    application
}

dependencies {
    implementation(project(":bridge-api"))
    implementation(project(":fabric-metadata"))
    implementation(project(":integration-harness"))
    implementation(project(":scenario-yaml"))
    implementation(project(":compatibility-catalog"))
    runtimeOnly(project(":fabric-remap"))
    runtimeOnly(project(":fabric-api-base-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-command-api-v2-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-lifecycle-events-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-resource-loader-v0-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-resource-conditions-api-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-loot-api-v3-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-recipe-api-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-entity-events-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-data-attachment-api-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-game-rule-api-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-object-builder-api-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-api-lookup-api-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-registry-sync-v0-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-networking-api-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-transfer-api-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-content-registries-v0-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-item-group-api-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-convention-tags-v2-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-biome-api-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-blockrenderlayer-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":fabric-rendering-v1-bridge")) {
        isTransitive = false
    }
    runtimeOnly(project(":repository-modrinth"))
    runtimeOnly(project(":repository-curseforge"))
    implementation("info.picocli:picocli:4.7.6")
    implementation("com.google.code.gson:gson:2.10.1")
}

application {
    mainClass.set("dev.loaderbridge.cli.LoaderBridgeCli")
}
