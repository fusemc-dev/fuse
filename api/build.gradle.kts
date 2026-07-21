plugins {
    `java-library`
    id("fabric-loom") version "1.15-SNAPSHOT"
}

val artifact: String by project

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

base {
    archivesName = "$artifact-api"
}

repositories {
    mavenLocal()
}

val graalVersion: String by project
val minecraftVersion: String by project

dependencies {
    api("dev.fusemc:tau:0.2.11")
    api("dev.fusemc:iota:0.2.3")
    api("dev.fusemc:quelle:0.1.3")
    api("com.manchickas:optionated:2.0.2")
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