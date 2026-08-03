plugins {
    `java-library`
}

dependencies {
    api(project(":bridge-api"))
    implementation(project(":fabric-metadata"))
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-tree:9.7.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.fabricmc:tiny-remapper:0.10.4")
    implementation("net.fabricmc:mapping-io:0.6.1")
    implementation("net.fabricmc:access-widener:2.1.0")
    runtimeOnly("net.fabricmc:intermediary:1.21.1:v2")
    testRuntimeOnly(project(":fabric-api-base-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-command-api-v2-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-lifecycle-events-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-resource-loader-v0-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-game-rule-api-v1-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-object-builder-api-v1-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-api-lookup-api-v1-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-registry-sync-v0-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-networking-api-v1-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-transfer-api-v1-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-content-registries-v0-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-item-group-api-v1-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-convention-tags-v2-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-biome-api-v1-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-blockrenderlayer-v1-bridge")) {
        isTransitive = false
    }
    testRuntimeOnly(project(":fabric-rendering-v1-bridge")) {
        isTransitive = false
    }
}
