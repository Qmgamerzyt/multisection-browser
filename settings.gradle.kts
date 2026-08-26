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
        google()
        mavenCentral()
        maven("https://dl.google.com/dl/android/maven2/")
        maven("https://maven.mozilla.org/maven2/")
    }
}
rootProject.name = "MultiSectionBrowser"
include(":app")