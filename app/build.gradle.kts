plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("lernen.android.hilt")
}

android {
    namespace = "de.lernen.app"
    compileSdk = Konfig.COMPILE_SDK

    defaultConfig {
        applicationId = "de.lernen.app"
        minSdk = Konfig.MIN_SDK
        targetSdk = Konfig.TARGET_SDK
        versionCode = 4
        versionName = "0.4.0"
    }

    // Signatur für Releases: nur über Umgebungsvariablen (GitHub-Secrets), nie im Repository.
    val keystore = System.getenv("LERNEN_KEYSTORE")?.let(::file)?.takeIf { it.exists() }
    signingConfigs {
        if (keystore != null) create("release") {
            storeFile = keystore
            storePassword = System.getenv("LERNEN_KEYSTORE_PASSWORT")
            keyAlias = System.getenv("LERNEN_KEY_ALIAS")
            keyPassword = System.getenv("LERNEN_KEY_PASSWORT")
        }
    }

    buildTypes {
        release {
            // Ohne Keystore (lokal): mit Debug-Schlüssel signieren, damit die APK installierbar ist.
            signingConfig = if (keystore != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = Konfig.JAVA
        targetCompatibility = Konfig.JAVA
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core-design"))
    implementation(project(":core-data"))
    implementation(project(":core-sync"))
    implementation(project(":feature-start"))
    implementation(project(":feature-faecher"))
    implementation(project(":feature-thema"))
    implementation(project(":feature-katalog"))
    implementation(project(":feature-fragen"))
    implementation(project(":feature-werkzeuge"))
    implementation(project(":feature-einstellungen"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.navsuite)
    implementation(libs.compose.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    // Rauchtest der ganzen App auf der JVM (Robolectric), weil kein Emulator nötig sein soll
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
