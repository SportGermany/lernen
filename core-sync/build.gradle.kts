// Sync mit dem Paket-Repo (lesen über raw.githubusercontent.com) und GitHub-Contents-API (Fragen)
plugins {
    id("lernen.android.library")
    id("lernen.android.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android { namespace = "de.lernen.core.sync" }

dependencies {
    implementation(project(":core-data"))
    api(libs.okhttp)
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
