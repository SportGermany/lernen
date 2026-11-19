// Reines Kotlin-Modul ohne Android (core-math, core-units)
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = Konfig.JAVA
    targetCompatibility = Konfig.JAVA
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}
