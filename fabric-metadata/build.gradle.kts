plugins {
    `java-library`
}

dependencies {
    api(project(":bridge-api"))
    implementation("com.google.code.gson:gson:2.10.1")
}
