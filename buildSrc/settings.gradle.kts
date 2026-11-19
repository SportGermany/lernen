dependencyResolutionManagement {
    repositories {
        google()
        // Spiegel von Maven Central (weniger Drosselung), danach das Original
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
rootProject.name = "buildSrc"
