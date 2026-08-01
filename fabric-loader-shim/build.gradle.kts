plugins {
    `java-library`
}

dependencies {
    implementation(project(":bridge-api"))
}

tasks.jar {
    manifest {
        attributes(
            "Automatic-Module-Name" to "dev.loaderbridge.fabric.runtime",
            "FMLModType" to "LIBRARY",
            "LICENSE" to "Apache-2.0",
        )
    }
}
