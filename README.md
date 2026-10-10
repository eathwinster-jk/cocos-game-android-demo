# Cocos Game Android Demo

独立 Android 接入示例，不包含本地 AAR，通过 GitHub Maven 仓库拉取 SDK。

```kotlin
repositories {
    maven {
        url = uri("https://raw.githubusercontent.com/eathwinster-jk/cocos-game-android-sdk/main/repository")
    }
}

dependencies {
    implementation("com.eath.cocosgamesdk:game-sdk:0.1.0")
}
```

运行后由宿主输入 UID、Token 和场景 ID，再通过 `GameSessionConfig` 打开游戏。

构建：

```shell
./gradlew :app:assembleDebug
```
