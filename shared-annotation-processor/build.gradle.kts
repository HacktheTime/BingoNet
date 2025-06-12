import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    java
    idea
    `java-library`
    id("application")
}

group = "de.hype.bingonet.shared.compiler"
version = "1.0-SNAPSHOT"

tasks.test {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://jitpack.io")
    maven("https://repo.nea.moe/releases")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
    //implementation("org.apache.logging.log4j:log4j-Core.INSTANCE:2.20.0")
    implementation("com.google.code.gson:gson")
//    implementation("io.github.JDA-Fork:JDA:82d7ab90d6")
    implementation("net.dv8tion:JDA:latest.release")
    implementation("org.commonmark:commonmark:latest.release")
    implementation("org.reflections:reflections:latest.release")
    implementation("com.github.HacktheTime:HypixelAPI:4f36fb94e2d393121e7b0181d4e1f6b99b59394e")
//    implementation("com.sun.mail:javax.mail")
    implementation("me.nullicorn:Nedit:latest.release")
    implementation("org.apache.commons:commons-lang3:latest.release")
    implementation("org.apache.commons:commons-text:latest.release")
    implementation("ch.qos.logback:logback-classic:latest.release")
    implementation("org.jsoup:jsoup:latest.release")
    implementation(kotlin("stdlib"))
    implementation("com.google.auto.service:auto-service:latest.release")
    annotationProcessor("com.google.auto.service:auto-service:latest.release")
    kapt("com.google.auto.service:auto-service:latest.release")
    implementation("com.github.javaparser:javaparser-core:latest.release")
    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.squareup:kotlinpoet:latest.release")
    implementation("com.google.devtools.ksp:symbol-processing-api:latest.release")
    implementation("com.squareup:kotlinpoet-ksp:latest.release")
    implementation("org.eclipse.jgit:org.eclipse.jgit:latest.release")
    api("com.github.HacktheTime:RepoParser:master-SNAPSHOT")
//    implementation("moe.nea:neurepoparser:latest.release")
}

application {
    mainClass.set("de.hype.bingonet.shared.compilation.Main")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-AprojectDir=${project.projectDir}")
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xmulti-dollar-interpolation"))
}