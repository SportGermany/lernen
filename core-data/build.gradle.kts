plugins {
    id("lernen.android.library")
    id("lernen.android.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.room")
}

android { namespace = "de.lernen.core.data" }

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.json.schema.validator)
}
