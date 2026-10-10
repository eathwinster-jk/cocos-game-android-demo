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
        maven {
            url = uri("https://raw.githubusercontent.com/eathwinster-jk/cocos-game-android-sdk/main/repository")
        }
    }
}
rootProject.name = "CocosGameAndroidDemo"
include(":app")
