plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":fabric-loader-shim"))
}

tasks.jar {
    archiveBaseName.set("loaderbridge-fabric-nested-child-fixture")
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}
