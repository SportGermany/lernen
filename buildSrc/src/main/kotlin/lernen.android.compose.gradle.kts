// Android-Bibliothek mit Compose (core-design)
plugins {
    id("lernen.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    buildFeatures { compose = true }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    val bom = platform(libs.findLibrary("compose-bom").get())
    "implementation"(bom)
    "implementation"(libs.findLibrary("compose-ui").get())
    "implementation"(libs.findLibrary("compose-material3").get())
    "implementation"(libs.findLibrary("compose-icons-core").get())
    "implementation"(libs.findLibrary("compose-ui-tooling-preview").get())
    "debugImplementation"(libs.findLibrary("compose-ui-tooling").get())
}
