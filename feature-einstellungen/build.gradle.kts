plugins { id("lernen.android.feature") }

android { namespace = "de.lernen.feature.einstellungen" }

dependencies {
    implementation(project(":core-sync"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
