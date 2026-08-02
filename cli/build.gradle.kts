plugins {
    application
}

dependencies {
    implementation(project(":bridge-api"))
    implementation(project(":fabric-metadata"))
    implementation(project(":integration-harness"))
    runtimeOnly(project(":fabric-remap"))
    implementation("info.picocli:picocli:4.7.6")
    implementation("com.google.code.gson:gson:2.10.1")
}

application {
    mainClass.set("dev.loaderbridge.cli.LoaderBridgeCli")
}
