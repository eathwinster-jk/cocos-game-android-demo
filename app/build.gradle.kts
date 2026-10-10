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
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "GAME_WS_URL", "\"wss://test.imnono.net/game-ws/ws\"")
        buildConfigField("String", "GAME_API_URL", "\"https://test.imnono.net/game-lobby\"")
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
    implementation("com.eath.cocosgamesdk:game-sdk:0.1.0")
}
