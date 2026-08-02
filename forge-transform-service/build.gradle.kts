plugins {
    `java-library`
}

dependencies {
    compileOnly("net.minecraftforge:modlauncher:10.2.4")
}

tasks.jar {
    manifest {
        attributes(
            "Automatic-Module-Name" to "dev.loaderbridge.forge.transform",
            "LICENSE" to "Apache-2.0",
            "Implementation-Title" to "LoaderBridge Transform Service",
            "Implementation-Vendor" to "LoaderBridge contributors",
            "Implementation-Version" to "0.1.0",
        )
    }
}

tasks.test {
    dependsOn(tasks.jar)
    systemProperty("loaderbridge.transformJar", tasks.jar.flatMap { it.archiveFile }.get().asFile.absolutePath)
}
