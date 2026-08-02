plugins {
    `java-library`
}

dependencies {
    implementation(project(":bridge-api"))
    implementation(project(":fabric-metadata"))
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.jar {
    dependsOn(":bridge-api:classes", ":fabric-metadata:classes")
    from(project(":bridge-api").layout.buildDirectory.dir("classes/java/main"))
    from(project(":fabric-metadata").layout.buildDirectory.dir("classes/java/main"))
    manifest {
        attributes(
            "Automatic-Module-Name" to "dev.loaderbridge.fabric.runtime",
            "FMLModType" to "LIBRARY",
            "LICENSE" to "Apache-2.0",
        )
    }
}

tasks.test {
    dependsOn(tasks.jar)
    systemProperty("loaderbridge.shimJar", tasks.jar.flatMap { it.archiveFile }.get().asFile.absolutePath)
}
