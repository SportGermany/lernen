pluginManagement {
    repositories {
        google()
        // Spiegel von Maven Central (weniger Drosselung), danach das Original
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Spiegel von Maven Central (weniger Drosselung), danach das Original
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}

rootProject.name = "lernen"

include(
    ":app",
    ":core-design",
    ":core-data",
    ":core-sync",
    ":core-math",
    ":core-units",
    ":feature-start",
    ":feature-faecher",
    ":feature-thema",
    ":feature-katalog",
    ":feature-fragen",
    ":feature-werkzeuge",
    ":feature-einstellungen",
)
