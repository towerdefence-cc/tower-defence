import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("application")
}

group = "pink.zak.minestom"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.towerdefence.cc/snapshots")
    maven("https://repo.towerdefence.cc/releases")
    maven("https://jitpack.io")
}

dependencies {
    implementation("cc.towerdefence.minestom:core:3e7ecb5")
    implementation("net.kyori:adventure-text-minimessage:4.11.0")

    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")

    testImplementation("cc.towerdefence.minestom:core:53b1fad")

    //testImplementation("net.minestom.server:Minestom:1.0")
}

application {
    mainClass.set("pink.zak.minestom.towerdefence.TowerDefenceServer")
}

tasks.named<ShadowJar>("shadowJar") {
    mergeServiceFiles()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}