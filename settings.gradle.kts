pluginManagement {
    repositories {
        // Google Maven 镜像（maven.google.com 在部分网络不可达，使用 dl.google.com 镜像）
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        mavenCentral()
    }
}

rootProject.name = "FurryLive2DLauncher"
include(":app")
