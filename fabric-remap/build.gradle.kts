plugins {
    `java-library`
}

dependencies {
    api(project(":bridge-api"))
    implementation(project(":fabric-metadata"))
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-tree:9.7.1")
    implementation("com.google.code.gson:gson:2.10.1")
}
