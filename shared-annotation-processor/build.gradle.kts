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
    api("com.google.code.gson:gson")
//    implementation("io.github.JDA-Fork:JDA:82d7ab90d6")
    api("net.dv8tion:JDA:latest.release")
    api("org.commonmark:commonmark:latest.release")
    api("org.reflections:reflections:latest.release")
    api("com.github.HacktheTime:HypixelAPI:4f36fb94e2d393121e7b0181d4e1f6b99b59394e")
//    implementation("com.sun.mail:javax.mail")
    api("me.nullicorn:Nedit:latest.release")
    api("org.apache.commons:commons-lang3:latest.release")
    api("org.apache.commons:commons-text:latest.release")
    api("ch.qos.logback:logback-classic:latest.release")
    api("org.jsoup:jsoup:latest.release")
    api(kotlin("stdlib"))
    implementation("com.google.auto.service:auto-service:latest.release")
    annotationProcessor("com.google.auto.service:auto-service:latest.release")
    kapt("com.google.auto.service:auto-service:latest.release")
    api("com.github.javaparser:javaparser-core:latest.release")
    api("com.squareup:javapoet:1.13.0")
    api("com.squareup:kotlinpoet:latest.release")
    api("com.google.devtools.ksp:symbol-processing-api:latest.release")
    api("com.squareup:kotlinpoet-ksp:latest.release")
    api("org.eclipse.jgit:org.eclipse.jgit:latest.release")
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