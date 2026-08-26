pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://dl.google.com/dl/android/maven2/")
        maven("https://maven.mozilla.org/maven2/")
    }
}
rootProject.name = "MultiSectionBrowser"
include(":app")