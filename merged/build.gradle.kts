plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("application")
}

group = "me.merged"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.jgrapht:jgrapht-core:1.5.1")
    implementation("commons-io:commons-io:2.6")
    implementation("com.github.ajalt.clikt:clikt:3.1.0")
}

application {
    mainClass.set("merged.MergedProject")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    shadowJar {
        archiveBaseName.set("merged-java-shadow")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "merged.MergedProject"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}