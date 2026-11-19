plugins { id("lernen.android.compose") }

android { namespace = "de.lernen.core.design" }

dependencies {
    implementation(project(":core-data"))
    implementation(libs.androidx.core.ktx)
}
