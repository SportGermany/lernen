plugins { id("lernen.android.feature") }

android { namespace = "de.lernen.feature.start" }

dependencies {
    implementation(project(":core-sync"))
}
