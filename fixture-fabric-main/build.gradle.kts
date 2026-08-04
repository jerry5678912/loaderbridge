plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":fabric-loader-shim"))
    compileOnly(project(":fixture-fabric-nested-child"))
    compileOnly("org.spongepowered:mixin:0.8.7")
    compileOnly("org.ow2.asm:asm-tree:9.7.1")
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.4")
}

val nestedChildJar = project(":fixture-fabric-nested-child").tasks.named<Jar>("jar")

tasks.jar {
    dependsOn(nestedChildJar)
    archiveBaseName.set("loaderbridge-fabric-main-fixture")
    from(nestedChildJar.flatMap { it.archiveFile }) {
        into("META-INF/jars")
        rename { "loaderbridge-nested-child.jar" }
    }
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

tasks.test {
    dependsOn(tasks.jar)
    systemProperty(
        "loaderbridge.fixtureJar",
        tasks.jar.flatMap { it.archiveFile }.get().asFile.absolutePath,
    )
}
