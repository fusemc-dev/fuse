plugins {
    `java-library`
    id("fabric-loom") version "1.15-SNAPSHOT"
}

val artifact: String by project

group = providers.gradleProperty("group")
    .get()
version = providers.gradleProperty("version")
    .get()

base {
    archivesName = "$artifact-api"
}

repositories {
    mavenLocal()
}

val graalVersion: String by project
val minecraftVersion: String by project

dependencies {
    api("com.manchickas:jet:1.2.0")
    implementation("com.manchickas:quelle:1.1.1")
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.officialMojangMappings())
    implementation("org.graalvm.polyglot:polyglot:${graalVersion}")
    implementation("org.graalvm.js:js-language:${graalVersion}")
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}