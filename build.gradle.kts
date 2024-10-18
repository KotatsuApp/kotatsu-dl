import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.0.20"
    id("com.gradleup.shadow") version "8.3.3"
}

group = "org.koitharu"
version = "0.1"

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.koitharu.kotatsu.dl.MainKt"
    }
}

tasks.withType<ShadowJar> {
    archiveBaseName = "kotatsu-dl"
    archiveClassifier = ""
    archiveVersion = ""
    minimize()
}

repositories {
    mavenCentral()
    google()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
    implementation("com.github.ajalt.clikt:clikt-core:5.0.1")
    implementation("com.github.KotatsuApp:kotatsu-parsers:08fe54c36d")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("io.webfolder:quickjs:1.1.0")
    implementation("org.json:json:20240303")
    implementation("me.tongfei:progressbar:0.10.1")
    implementation("androidx.collection:collection:1.4.4")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}