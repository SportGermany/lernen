plugins {
    id("lernen.android.feature")
    // Bildschirmfotos der Oberfläche als JVM-Test (ohne Emulator)
    id("app.cash.paparazzi")
}

android { namespace = "de.lernen.feature.thema" }
