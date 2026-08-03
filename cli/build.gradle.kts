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
    runtimeOnly(project(":repository-modrinth"))
    runtimeOnly(project(":repository-curseforge"))
    implementation("info.picocli:picocli:4.7.6")
    implementation("com.google.code.gson:gson:2.10.1")
}

application {
    mainClass.set("dev.loaderbridge.cli.LoaderBridgeCli")
}
