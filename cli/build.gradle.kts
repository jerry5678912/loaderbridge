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
    runtimeOnly(project(":repository-modrinth"))
    runtimeOnly(project(":repository-curseforge"))
    implementation("info.picocli:picocli:4.7.6")
    implementation("com.google.code.gson:gson:2.10.1")
}

application {
    mainClass.set("dev.loaderbridge.cli.LoaderBridgeCli")
}
