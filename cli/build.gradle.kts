plugins {
    application
}

dependencies {
    implementation(project(":bridge-api"))
    implementation(project(":fabric-metadata"))
    runtimeOnly(project(":fabric-remap"))
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")
}

application {
    mainClass.set("dev.loaderbridge.cli.LoaderBridgeCli")
}
