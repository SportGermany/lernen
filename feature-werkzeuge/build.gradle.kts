plugins {
    id("lernen.android.feature")
    id("app.cash.paparazzi")
}

android { namespace = "de.lernen.feature.werkzeuge" }

dependencies {
    implementation(project(":core-math"))
    implementation(project(":core-units"))
}
