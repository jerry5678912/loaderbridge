plugins {
    `java-library`
}

dependencies {
    api(project(":bridge-api"))
    implementation(project(":fabric-metadata"))
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-tree:9.7.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.fabricmc:tiny-remapper:0.10.4")
    implementation("net.fabricmc:mapping-io:0.6.1")
    implementation("net.fabricmc:access-widener:2.1.0")
    runtimeOnly("net.fabricmc:intermediary:1.21.1:v2")
}
