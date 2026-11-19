plugins { id("lernen.android.feature") }

android { namespace = "de.lernen.feature.katalog" }

dependencies {
    implementation(project(":core-sync"))
}
