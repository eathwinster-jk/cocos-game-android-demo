pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    val gameSdkRepositoryUrl = providers.gradleProperty("gameSdkRepositoryUrl")
        .orElse("https://raw.githubusercontent.com/eathwinster-jk/cocos-game-android-sdk/main/repository")
        .get()
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri(gameSdkRepositoryUrl)
        }
    }
}
rootProject.name = "CocosGameAndroidDemo"
include(":app")
