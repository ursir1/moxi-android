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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MoxiApp"
include(":app")
include(":mpv-android-lib")
project(":mpv-android-lib").projectDir = file("third_party/mpv-android-lib/app")
if (file("streamio-core-kotlin/stremio-core-kotlin").exists()) {
    includeBuild("streamio-core-kotlin/stremio-core-kotlin")
}
