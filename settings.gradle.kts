pluginManagement {
    // 包含本地插件模块 仅测试
    //includeBuild("aniflux-gradle-plugin")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "aniflux"
include(":app")
include(":aniflux")
include(":aniflux-core")
include(":aniflux-annotations")
include(":aniflux-compiler-ksp")
include(":aniflux-gif")
include(":aniflux-pag")
include(":aniflux-lottie")
include(":aniflux-svga")
include(":aniflux-vap")

include(":animLibs:svga")
include(":animLibs:android-gif-drawable")
include(":animLibs:vap")
include(":animLibs:libpag:android:libpag")
include(":animLibs:lottie")



include(":aniflux-gradle-plugin")