// Feature-Modul: Compose + Hilt + Navigation + Zugriff auf Design und Daten
plugins {
    id("lernen.android.compose")
    id("lernen.android.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(project(":core-design"))
    "implementation"(project(":core-data"))
    "implementation"(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
    "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
    "implementation"(libs.findLibrary("androidx-navigation-compose").get())
    "implementation"(libs.findLibrary("hilt-navigation-compose").get())
    "implementation"(libs.findLibrary("kotlinx-serialization-json").get())
    "testImplementation"(libs.findLibrary("junit").get())
}
