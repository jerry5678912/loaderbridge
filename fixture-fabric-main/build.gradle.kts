plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":fabric-loader-shim"))
    compileOnly("org.spongepowered:mixin:0.8.7")
    compileOnly("org.ow2.asm:asm-tree:9.7.1")
}

tasks.jar {
    archiveBaseName.set("loaderbridge-fabric-main-fixture")
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}
