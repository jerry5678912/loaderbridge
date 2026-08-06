plugins {
    `java-library`
}

dependencies {
    compileOnly("net.minecraftforge:modlauncher:10.2.4")
    compileOnly("org.ow2.asm:asm-tree:9.7.1")
    implementation("net.fabricmc:access-widener:2.1.0")
    testImplementation("net.minecraftforge:modlauncher:10.2.4") {
        isTransitive = false
    }
    testImplementation("org.ow2.asm:asm-tree:9.7.1")
}

val embedded by configurations.creating

dependencies {
    embedded("net.fabricmc:access-widener:2.1.0") {
        isTransitive = false
    }
}

tasks.jar {
    from(embedded.map { dependency -> zipTree(dependency) }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "module-info.class")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
