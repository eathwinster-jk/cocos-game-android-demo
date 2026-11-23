plugins {
    id("com.android.application")
}

android {
    namespace = "com.eath.cocosgamedemo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eath.cocosgamedemo"
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "0.1.11"
        buildConfigField("String", "GAME_WS_URL", "\"wss://test.imnono.net/game-ws/ws\"")
        buildConfigField("String", "GAME_API_URL", "\"https://test.imnono.net/game-lobby\"")
        buildConfigField("String", "GAME_GUEST_SCENE_ID", "\"3947124\"")
        buildConfigField("String", "LOGIN_API_URL", "\"https://test2.kzb001.net/prod-api/login\"")
        buildConfigField("int", "LOGIN_PID", "4")
        buildConfigField("String", "LOGIN_VERSION", "\"1.120\"")
        buildConfigField("String", "LOGIN_USER_AGENT", "\"cocos-game-sdk-demo/1.120\"")
        buildConfigField("int", "LOGIN_APP_TYPE", "1")
        buildConfigField("String", "LOGIN_COUNTRY_CODE", "\"86\"")
        // 游戏模块热更新版本清单地址（0.7.0 起 SDK 不内置游戏，必须配置）
        buildConfigField("String", "MODULE_CATALOG_URL", "\"https://cocos-game-modules.pages.dev/android/catalog.json\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    defaultConfig {
        ndk {
            abiFilters += "arm64-v8a"
        }
    }
}

dependencies {
    val gameSdkVersion = providers.gradleProperty("gameSdkVersion")
        .orElse("0.7.0")
        .get()
    implementation("com.eath.cocosgamesdk:game-sdk:$gameSdkVersion")
    implementation("com.eath.cocosgamesdk:game-sdk-hotupdate:$gameSdkVersion")
}
