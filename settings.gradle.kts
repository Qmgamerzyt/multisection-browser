pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        maven("https://maven.google.com")
        maven("https://dl.google.com/dl/android/maven2/")
        maven("https://maven.mozilla.org/maven2/")
    }
}
rootProject.name = "MultiSectionBrowser"
include(":app")