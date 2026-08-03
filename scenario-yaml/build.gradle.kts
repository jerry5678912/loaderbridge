plugins {
    `java-library`
}

dependencies {
    api(project(":scenario-api"))
    implementation("org.snakeyaml:snakeyaml-engine:3.0.1")
}

tasks.test {
    systemProperty("loaderbridge.controlledScenarios",
        rootProject.layout.projectDirectory.dir("scenarios/controlled").asFile.absolutePath)
    systemProperty("loaderbridge.realModScenarios",
        rootProject.layout.projectDirectory.dir("scenarios/real-mod").asFile.absolutePath)
}
