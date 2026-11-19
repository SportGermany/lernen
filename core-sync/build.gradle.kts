// Meilenstein 2: Sync von katalog.json, faecher.json und Paketen
plugins { id("lernen.android.library") }

android { namespace = "de.lernen.core.sync" }

dependencies {
    implementation(project(":core-data"))
}
