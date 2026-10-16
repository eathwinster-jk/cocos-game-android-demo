# Cocos Game Android SDK 接入文档

## 1. SDK 信息

- Maven 坐标：`com.eath.cocosgamesdk:game-sdk:0.1.3`
- SDK 仓库：https://github.com/eathwinster-jk/cocos-game-android-sdk
- 当前模块：`niuniu`
- ABI：`arm64-v8a`
- `compileSdkVersion`：35
- `minSdkVersion`：26
- `targetSdkVersion`：34
- Java：JDK 17 构建，Java 8 代码兼容

宿主客户端必须提供：

- `liveUserId`
- `imUserId`
- `liveToken`
- `sceneType`
- `sceneId`
- WebSocket 地址
- 大厅 API 地址

SDK 不负责生产登录，也不内置固定 UID、Token 或 Scene ID。

## 2. 接入模式一：本地 AAR

### 2.1 下载 AAR

从 SDK 仓库下载：

```text
repository/com/eath/cocosgamesdk/game-sdk/0.1.3/game-sdk-0.1.3.aar
```

复制到宿主：

```text
app/libs/game-sdk-0.1.3.aar
```

### 2.2 Groovy 配置

`app/build.gradle`：

```groovy
android {
    compileSdkVersion 35

    defaultConfig {
        minSdkVersion 26
        targetSdkVersion 34
        ndk {
            abiFilters "arm64-v8a"
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging true
        }
    }
}

dependencies {
    implementation files("libs/game-sdk-0.1.3.aar")
}
```

### 2.3 Kotlin DSL 配置

`app/build.gradle.kts`：

```kotlin
android {
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(files("libs/game-sdk-0.1.3.aar"))
}
```

## 3. 接入模式二：Gradle 远程拉取

推荐使用该模式。

### 3.1 Groovy 配置

项目 `settings.gradle`：

```groovy
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
```

模块 `app/build.gradle`：

```groovy
dependencies {
    implementation "com.eath.cocosgamesdk:game-sdk:0.1.3"
}
```

### 3.2 Kotlin DSL 配置

项目 `settings.gradle.kts`：

```kotlin
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
```

模块 `app/build.gradle.kts`：

```kotlin
dependencies {
    implementation("com.eath.cocosgamesdk:game-sdk:0.1.3")
}
```

Gradle Sync 时会自动下载 AAR，不需要提交二进制文件。

## 4. Manifest

SDK AAR 会合并：

- 网络权限
- 网络状态权限
- 震动权限
- `CocosGameActivity`
- `CocosEditBoxActivity`

宿主入口 Activity 正常声明即可：

```xml
<application
    android:allowBackup="false"
    android:theme="@android:style/Theme.Material.Light.NoActionBar">
    <activity
        android:name=".MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN"/>
            <category android:name="android.intent.category.LAUNCHER"/>
        </intent-filter>
    </activity>
</application>
```

### 4.1 权限

SDK AAR 已声明：

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.VIBRATE"/>
```

宿主不需要重复声明。通知、相机、麦克风和存储权限不是当前游戏 SDK 的必需权限。

### 4.2 混淆配置

AAR 已携带 Consumer ProGuard。宿主有额外收缩规则时，建议确认以下配置：

```proguard
-keep class com.eath.cocosgamesdk.** { *; }
-keep class com.cocos.lib.** { *; }
-dontwarn com.cocos.lib.**
```

## 5. 初始化 SDK

建议在主进程的 Application 中初始化，且只初始化一次。必须在调用其他 SDK 接口前完成。

### 5.1 Java

```java
public final class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CocosGameSDK.getInstance().initialize(this);
    }
}
```

### 5.2 Kotlin

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CocosGameSDK.getInstance().initialize(this)
    }
}
```

如果未自定义 Application，也可以在首个 Activity 初始化，但必须保证打开游戏前已经执行。

## 6. 客户端动态配置

客户端登录成功后自行准备：

```text
liveUserId：直播用户 UID（登录响应 uid，用于大厅鉴权）
imUserId：IM 用户 UID（登录响应 userinfo.imUserId，用于识别本人座位）
liveToken：当前有效 Token
sceneType：1 直播间，2 IM 群
sceneId：当前直播间或群编号
tableId：可选，0 表示自动选择
```

禁止将 UID、Token、Scene ID 固定写死在 SDK。

## 7. Java 打开游戏

```java
private void openGame(
    long liveUserId,
    long imUserId,
    String liveToken,
    int sceneType,
    String sceneId) {
    GameSessionConfig session = new GameSessionConfig(
        "wss://test.imnono.net/game-ws/ws",
        "https://test.imnono.net/game-lobby",
        liveUserId,
        imUserId,
        liveToken,
        sceneType,
        sceneId,
        0,
        BuildConfig.VERSION_NAME,
        "Android"
    );

    CocosGameSDK.getInstance().openGame(
        this,
        "niuniu",
        session
    );
}
```

## 8. Kotlin 打开游戏

```kotlin
private fun openGame(
    liveUserId: Long,
    imUserId: Long,
    liveToken: String,
    sceneType: Int,
    sceneId: String,
) {
    val session = GameSessionConfig(
        "wss://test.imnono.net/game-ws/ws",
        "https://test.imnono.net/game-lobby",
        liveUserId,
        imUserId,
        liveToken,
        sceneType,
        sceneId,
        0,
        BuildConfig.VERSION_NAME,
        "Android",
    )

    CocosGameSDK.getInstance().openGame(
        this,
        "niuniu",
        session,
    )
}
```

`GameSessionConfig` 会校验：

- WebSocket 必须为 `wss://`
- API 必须为 `https://`
- UID 必须大于 0
- Token 不能为空
- Scene Type 必须为 1 或 2
- Scene ID 不能为空

## 9. 处理会话失效

当前 SDK 使用固定请求码：

```text
CocosGameSDK.GAME_REQUEST_CODE = 7301
```

### 9.1 Java

```java
@Override
protected void onActivityResult(
    int requestCode,
    int resultCode,
    Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode != CocosGameSDK.GAME_REQUEST_CODE) return;
    if (resultCode != Activity.RESULT_CANCELED || data == null) return;
    if (!data.hasExtra(CocosGameActivity.EXTRA_SESSION_ERROR)) return;

    String message = data.getStringExtra(
        CocosGameActivity.EXTRA_SESSION_ERROR
    );
    clearLoginSession();
    openLoginPage(message);
}
```

### 9.2 Kotlin

```kotlin
override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode != CocosGameSDK.GAME_REQUEST_CODE) return
    if (resultCode != Activity.RESULT_CANCELED || data == null) return
    if (!data.hasExtra(CocosGameActivity.EXTRA_SESSION_ERROR)) return

    val message = data.getStringExtra(
        CocosGameActivity.EXTRA_SESSION_ERROR
    )
    clearLoginSession()
    openLoginPage(message)
}
```

## 10. 接收游戏公共事件

### 10.1 Java

```java
CocosGameSDK.getInstance().setHostListener(
    (gameId, event, payloadJson) -> {
        if ("wallet.open".equals(event)) {
            openWalletPage(payloadJson);
        }
    }
);
```

### 10.2 Kotlin

```kotlin
CocosGameSDK.getInstance().setHostListener { gameId, event, payloadJson ->
    if (event == "wallet.open") {
        openWalletPage(payloadJson)
    }
}
```

Activity 销毁时清除：

### Java

```java
CocosGameSDK.getInstance().setHostListener(null);
```

### Kotlin

```kotlin
CocosGameSDK.getInstance().setHostListener(null)
```

## 11. 客户端通知游戏

### Java

```java
CocosGameSDK.getInstance().sendGameEvent(
    "wallet.closed",
    "{\"isBalanceChanged\":true}"
);
```

### Kotlin

```kotlin
CocosGameSDK.getInstance().sendGameEvent(
    "wallet.closed",
    """{"isBalanceChanged":true}""",
)
```

事件名要求：

```text
^[a-z][a-z0-9_.-]{0,63}$
```

载荷必须是 JSON 对象。

## 12. 运行独立 Demo

```shell
git clone https://github.com/eathwinster-jk/cocos-game-android-demo.git
cd cocos-game-android-demo
./gradlew :app:assembleDebug
```

也可以使用 Android Studio：

1. 选择 Open。
2. 打开仓库根目录。
3. 选择 JDK 17。
4. 等待 Gradle Sync。
5. 连接 `arm64-v8a`、API 26 以上设备。
6. 运行 `app`。
7. 输入 UID、Token、Scene Type、Scene ID。
8. 点击“打开游戏”。

APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 13. 常见问题

### 13.1 Gradle 无法下载 SDK

检查是否能访问：

```text
https://raw.githubusercontent.com
```

清理后重试：

```shell
./gradlew --refresh-dependencies :app:assembleDebug
```

### 13.2 x86 模拟器无法运行

当前 SDK 只包含 `arm64-v8a`。使用 ARM 模拟器或 ARM64 真机。

### 13.3 sessionConfig 不能为空

UID、Token、Scene Type、Scene ID 必须由客户端登录和当前业务场景提供。

### 13.4 WebSocket 认证失败

检查 UID 与 Token 是否匹配、是否过期，以及 HTTP/WS 是否属于同一环境。

### 13.5 场景没有牛牛桌

检查 Scene Type、Scene ID，以及后台是否已在该场景开设牛牛桌。

## 14. 安全注意事项

- 不要将 UID、Token、账号密码提交到 Git。
- 不要在日志中打印 Token。
- 正式环境只使用 HTTPS/WSS。
- Token 失效后返回客户端登录流程。
- 不要使用动态版本号，例如 `game-sdk:+`。
- 生产环境固定 SDK 版本，并在升级前完成回归。
