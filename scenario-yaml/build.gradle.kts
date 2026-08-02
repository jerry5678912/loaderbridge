plugins {
    `java-library`
}

dependencies {
    api(project(":scenario-api"))
    implementation("org.snakeyaml:snakeyaml-engine:3.0.1")
}
