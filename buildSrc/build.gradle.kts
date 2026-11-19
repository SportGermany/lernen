plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.agp)
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.compose)
    implementation(libs.gradle.serialization)
    implementation(libs.gradle.ksp)
    implementation(libs.gradle.hilt)
    implementation(libs.gradle.room)
    implementation(libs.gradle.paparazzi)
}
