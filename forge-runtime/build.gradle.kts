plugins {
    `java-library`
}

dependencies {
    implementation(project(":bridge-api"))
    implementation(project(":fabric-loader-shim"))
    implementation("com.google.code.gson:gson:2.10.1")
    compileOnly("net.minecraftforge:forgespi:7.1.5")
    compileOnly("net.minecraftforge:modlauncher:10.2.4")
    compileOnly("net.minecraftforge:fmlcore:1.21.1-52.1.0")
    compileOnly("org.apache.maven:maven-artifact:3.9.9")
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
