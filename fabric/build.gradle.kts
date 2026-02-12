plugins {
    id("fabric-loom") version "1.15-SNAPSHOT"
    java
}

val loader: String by project
val artifact: String by project

group = providers.gradleProperty("group")
    .get()
version = providers.gradleProperty("version")
    .get()

base {
    archivesName = "$artifact-fabric"
}

repositories {
    mavenLocal()
}

val graalVersion: String by project
val minecraftVersion: String by project

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.officialMojangMappings())
    implementation("org.graalvm.polyglot:polyglot:${graalVersion}")
    implementation("org.graalvm.js:js-language:${graalVersion}")
    implementation("com.manchickas:jet:1.2.0")
    implementation("com.manchickas:quelle:1.1.1")
    implementation(project(path=":api", configuration="namedElements"))
    modImplementation("net.fabricmc:fabric-loader:0.18.2")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.139.4+1.21.11")
    include("org.graalvm.polyglot:polyglot:${graalVersion}")
    include("org.graalvm.js:js-language:${graalVersion}")
    include("org.graalvm.sdk:collections:${graalVersion}")
    include("org.graalvm.sdk:nativeimage:${graalVersion}")
    include("org.graalvm.sdk:word:${graalVersion}")
    include("org.graalvm.sdk:jniutils:${graalVersion}")
    include("org.graalvm.truffle:truffle-compiler:${graalVersion}")
    include("org.graalvm.truffle:truffle-runtime:${graalVersion}")
    include("org.graalvm.truffle:truffle-api:${graalVersion}")
    include("org.graalvm.regex:regex:${graalVersion}")
    include("org.graalvm.shadowed:icu4j:${graalVersion}")
    include("com.manchickas:jet:1.2.0")
    include("com.manchickas:quelle:1.1.1")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft", minecraftVersion)

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft" to minecraftVersion
        )
    }
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}