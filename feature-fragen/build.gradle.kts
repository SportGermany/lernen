plugins { id("lernen.android.feature") }

android { namespace = "de.lernen.feature.fragen" }

dependencies {
    implementation(project(":core-sync"))
}
