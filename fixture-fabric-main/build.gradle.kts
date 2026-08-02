plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":fabric-loader-shim"))
    compileOnly("org.spongepowered:mixin:0.8.7")
}

tasks.jar {
    archiveBaseName.set("loaderbridge-fabric-main-fixture")
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}
