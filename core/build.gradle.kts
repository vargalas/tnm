import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "me.xxx"
version = "1.0-SNAPSHOT"


dependencies {
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.jgrapht:jgrapht-core:1.5.1")
    implementation("commons-io:commons-io:2.6")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
