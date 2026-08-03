plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":fabric-loader-shim")) {
        isTransitive = false
    }
    compileOnly(project(":fabric-api-base-bridge")) {
        isTransitive = false
    }
}

tasks.jar {
    manifest {
        attributes("LICENSE" to "Apache-2.0")
    }
}
