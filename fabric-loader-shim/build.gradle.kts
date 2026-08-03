plugins {
    `java-library`
}

val fabricLoaderReference by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation(project(":bridge-api"))
    implementation(project(":fabric-metadata"))
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("org.ow2.asm:asm:9.7.1")
    fabricLoaderReference("net.fabricmc:fabric-loader:0.16.14") {
        isTransitive = false
    }
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
    inputs.files(fabricLoaderReference)
    systemProperty("loaderbridge.shimJar", tasks.jar.flatMap { it.archiveFile }.get().asFile.absolutePath)
    systemProperty(
        "loaderbridge.fabricLoaderReferenceJar",
        fabricLoaderReference.singleFile.absolutePath,
    )
}
