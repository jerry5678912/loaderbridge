plugins {
    `java-library`
}

dependencies {
    implementation(project(":bridge-api"))
}

tasks.jar {
    manifest {
        attributes(
            "Automatic-Module-Name" to "dev.loaderbridge.forge.runtime",
            "FMLModType" to "LANGPROVIDER",
            "LICENSE" to "Apache-2.0",
        )
    }
}
