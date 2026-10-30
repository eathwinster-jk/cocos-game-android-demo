# Cocos Game Android SDK 接入文档

## 1. SDK 信息

- Maven 坐标：`com.eath.cocosgamesdk:game-sdk:0.2.4`
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

SDK 不负责生产登录，也不内置固定 UID、Token 或 Scene ID。未登录用户可使用游客观战，见第 16 节。

## 2. 接入模式一：本地 AAR

### 2.1 下载 AAR

从 SDK 仓库下载：

```text
repository/com/eath/cocosgamesdk/game-sdk/0.2.4/game-sdk-0.2.4.aar
```

复制到宿主：

```text
app/libs/game-sdk-0.2.4.aar
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
    implementation files("libs/game-sdk-0.2.4.aar")
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
    implementation(files("libs/game-sdk-0.2.4.aar"))
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
    implementation "com.eath.cocosgamesdk:game-sdk:0.2.4"
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
    implementation("com.eath.cocosgamesdk:game-sdk:0.2.4")
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

## 12. 自定义游戏下方内容

竖屏时游戏画面下方依次是加注筹码卡片和宿主内容区，宿主内容（如直播、聊天）通过 `GameBottomContentProvider` 提供；全屏时自动隐藏，退出全屏恢复。未设置时内容区为空白。

在 `openGame` 之前设置：

### 12.1 Java

```java
CocosGameSDK.getInstance().setBottomContentProvider((activity, gameId) -> {
    // 返回 null 表示不显示
    return createHostBottomView(activity);
});
```

### 12.2 Kotlin

```kotlin
CocosGameSDK.getInstance().setBottomContentProvider { activity, gameId ->
    createHostBottomView(activity)
}
```

注意：

- 每次打开游戏页都会调用一次，返回新的 View
- 不要持有传入的 `Activity`，游戏页关闭后即失效
- 示例实现见 `app/src/main/java/com/eath/cocosgamedemo/DemoBottomContentProvider.java`

## 13. 迷你条

宿主可把牌桌缩成顶部 362:50 的横条，下方宿主内容随之上移。迷你条显示局数、阶段、头像、手牌与结算输赢、余额、展开和退出按钮；入座时显示自己，旁观时显示庄家。

- `setMiniMode(true)` 缩小，`setMiniMode(false)` 恢复；游戏页未打开时忽略。
- 玩家点击迷你条展开按钮时，SDK 自动恢复牌桌，宿主无需处理。
- 退出按钮与牌桌返回按钮行为一致。
- 迷你模式下不可切换全屏，处于全屏时进入迷你条会先退出全屏。
- 状态变化（宿主切换或玩家点击展开）通过 `GameHostListener.onMiniModeChanged` 回调。

### 13.1 Java

```java
CocosGameSDK.getInstance().setMiniMode(true);

CocosGameSDK.getInstance().setHostListener(new GameHostListener() {
    @Override
    public void onMiniModeChanged(String gameId, boolean isMiniMode) {
        // 同步宿主自己的按钮状态等
    }
});
```

### 13.2 Kotlin

```kotlin
CocosGameSDK.getInstance().setMiniMode(true)

CocosGameSDK.getInstance().setHostListener(object : GameHostListener {
    override fun onMiniModeChanged(gameId: String, isMiniMode: Boolean) {
        // 同步宿主自己的按钮状态等
    }
})
```

## 14. 嵌入宿主页面（View 接入）

保留宿主自己的直播详情页、群聊页，只把牌桌放进页面里的一个容器（与 iOS 一致）。牌桌铺满容器，容器尺寸由宿主控制，推荐宽高比 375:211。

- 宿主 Activity 须在 Manifest 声明 `android:configChanges="orientation|keyboardHidden|screenSize|screenLayout|smallestScreenSize"`，否则横屏全屏会重建页面，SDK 会拒绝全屏并打印错误日志。
- 宿主 `onBackPressed` 须先调用 `CocosGameSDK.getInstance().onBackPressed()`：全屏时退回竖屏、在座时由游戏确认离座，返回 false 再走宿主自己的返回。
- 横屏全屏：`toggleFullscreen()`，牌桌移到窗口顶层铺满并把页面转为横屏，退出时放回容器并恢复原方向。
- 迷你条：在 `onMiniModeChanged` 中把容器调整为 362:50（或恢复 375:211）。
- `closeGame()` 释放牌桌并移出容器；宿主页面销毁时 SDK 自动释放。同一时间只能打开一个牌桌。
- 钱包余额变化通过 `onGameEvent` 发送 `wallet.balance_changed`，载荷 `{"balanceCent":995420}`。

| `GameHostListener` 回调 | 说明 |
| --- | --- |
| `onGameEvent` | 通用事件，含钱包余额变化 |
| `onMiniModeChanged` | 迷你条变化，宿主调整容器高度 |
| `onFullscreenChanged` | 横屏全屏变化 |
| `onPlayingStateChanged` | 是否在座参与本局 |
| `onBackRequested` | 牌桌请求返回；返回 true 表示宿主自行处理并负责 `closeGame`，默认由 SDK 关闭 |
| `onSessionInvalid` | 会话失效，SDK 随后关闭牌桌 |
| `onGameClosed` | 牌桌已关闭并移出容器 |

示例见 `DemoEmbedActivity`，首页勾选「牌桌嵌入宿主页面」后进入。

### 14.1 Java

```java
CocosGameSDK.getInstance().setHostListener(this);
CocosGameSDK.getInstance().openGame(this, tableContainer, "niuniu", session);

@Override
public void onMiniModeChanged(String gameId, boolean isMiniMode) {
    tableContainer.setLayoutParams(createTableParams(isMiniMode));
}

@Override
public void onGameClosed(String gameId) {
    finish();
}

@Override
public void onBackPressed() {
    if (CocosGameSDK.getInstance().onBackPressed()) return;
    super.onBackPressed();
}
```

### 14.2 Kotlin

```kotlin
CocosGameSDK.getInstance().setHostListener(this)
CocosGameSDK.getInstance().openGame(this, tableContainer, "niuniu", session)

override fun onMiniModeChanged(gameId: String, isMiniMode: Boolean) {
    tableContainer.layoutParams = createTableParams(isMiniMode)
}

override fun onGameClosed(gameId: String) {
    finish()
}

override fun onBackPressed() {
    if (CocosGameSDK.getInstance().onBackPressed()) return
    super.onBackPressed()
}
```

## 15. 竖屏界面由宿主实现

竖屏下的下注、记录与规则、退出确认都可以由宿主自绘；注册对应监听后竖屏 SDK 不再展示这些界面，全屏仍由 SDK 展示。未注册的监听仍使用 SDK 自带界面。

| 界面 | 监听 | 宿主调用 |
| --- | --- | --- |
| 下注 | `setBetListener(GameBetListener)`：`onBetOptions`（注区、筹码、截止时间）、`onBetClosed`、`onBetResult` | `submitBet(roundId, areaCode, amountCent, clientBetNo)` |
| 记录与规则 | `setRecordsListener(GameRecordsListener)`：`onShowRecords`、`onRecordsUpdated` | `queryRecords(isHistory, filterKey, periodIndex, keyword)`、`loadMoreRecords(isHistory)` |
| 退出确认 | `setExitListener(GameExitListener)`：`onExitRequested` | `GameExitDecision.leave()` / `stay()` |

- `submitBet` 须在主线程调用；`clientBetNo` 为幂等号（不超过 64 个字符，可用 `CocosGameSDK.createClientBetNo()` 生成），同一幂等号在结果返回前或成功后再次提交会被忽略，失败后可用原幂等号重试。
- 退出确认必须调用 `leave()` 或 `stay()` 之一；游戏只允许在两局之间离座。
- 示例见 `DemoHostBetPanel`、`DemoHostRecordsSheet` 与 `DemoBottomContentProvider`。

### 15.1 Java

```java
CocosGameSDK.getInstance().setBetListener(new GameBetListener() {
    @Override
    public void onBetOptions(String gameId, GameBetOptions options) {
        // 展示注区、筹码，按 options.getDeadlineMillis() 倒计时
    }

    @Override
    public void onBetClosed(String gameId, String roundId) {
        // 收起下注面板
    }

    @Override
    public void onBetResult(String gameId, GameBetResult result) {
        // result.isSuccess()、result.getBalanceCent()、result.getErrorMessage()
    }
});
CocosGameSDK.getInstance().submitBet(roundId, areaCode, amountCent, CocosGameSDK.createClientBetNo());

CocosGameSDK.getInstance().setExitListener((gameId, decision) ->
    new AlertDialog.Builder(activity)
        .setTitle("确认离开牌桌？")
        .setPositiveButton("离开", (dialog, which) -> decision.leave())
        .setNegativeButton("继续游戏", (dialog, which) -> decision.stay())
        .setOnDismissListener(dialog -> decision.stay())
        .show());
```

### 15.2 Kotlin

```kotlin
CocosGameSDK.getInstance().setBetListener(object : GameBetListener {
    override fun onBetOptions(gameId: String, options: GameBetOptions) {
        // 展示注区、筹码，按 options.deadlineMillis 倒计时
    }

    override fun onBetClosed(gameId: String, roundId: String) {
        // 收起下注面板
    }

    override fun onBetResult(gameId: String, result: GameBetResult) {
        // result.isSuccess、result.balanceCent、result.errorMessage
    }
})
CocosGameSDK.getInstance().submitBet(roundId, areaCode, amountCent, CocosGameSDK.createClientBetNo())

CocosGameSDK.getInstance().setExitListener { _, decision ->
    AlertDialog.Builder(activity)
        .setTitle("确认离开牌桌？")
        .setPositiveButton("离开") { _, _ -> decision.leave() }
        .setNegativeButton("继续游戏") { _, _ -> decision.stay() }
        .setOnDismissListener { decision.stay() }
        .show()
}
```

## 16. 游客观战（直播间与群聊）

未登录用户可以游客身份进入直播间或 IM 群观战（群聊游客 SDK 0.2.4 起支持）。游客只能观战：

- `GameSessionConfig.createGuest(..., deviceId, sceneType, sceneId, ...)` 中 `sceneType` 传 1 直播间、2 IM 群；不带 `sceneType` 的旧重载默认直播间
- 宿主用设备唯一编号创建游客会话，`deviceId` 为 1~128 个字符，不能包含空白或控制字符
- 游客牌桌不显示右上角余额，下注阶段不弹下注窗，也不通知宿主
- 需要登录的操作统一通过 `onGameEvent` 发送 `auth.login_required`，由宿主提示登录：

| 载荷 | 触发操作 |
| --- | --- |
| `{"action":"join"}` | 点击立即加入 |
| `{"action":"records"}` | 点击记录与规则 |
| `{"action":"fullscreen"}` | 点击全屏（旋转大屏），不会进入全屏 |

- 事件名与 `action` 取值可直接用 SDK 0.2.2 起提供的常量：`GameEvents.LOGIN_REQUIRED`、`GameEvents.WALLET_BALANCE_CHANGED`、`GameEvents.LOGIN_ACTION_JOIN` / `LOGIN_ACTION_RECORDS` / `LOGIN_ACTION_FULLSCREEN`
- 建议留在游戏页提示登录或拉起登录页；登录成功后再 `closeGame()`，并用登录会话重新 `openGame`。不要在事件回调里直接关闭牌桌

### 16.1 Java

```java
String deviceId = "android-" + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
GameSessionConfig session = GameSessionConfig.createGuest(
    "wss://test.imnono.net/game-ws/ws",
    "https://test.imnono.net/game-lobby",
    deviceId,
    2,              // 场景类型：1 直播间，2 IM 群
    sceneId,        // 直播间编号或群号
    0,
    BuildConfig.VERSION_NAME,
    "Android"
);
CocosGameSDK.getInstance().openGame(this, tableContainer, "niuniu", session);

@Override
public void onGameEvent(String gameId, String event, String payloadJson) {
    if (GameEvents.LOGIN_REQUIRED.equals(event)) {
        showLoginPrompt();
    }
}
```

### 16.2 Kotlin

```kotlin
val deviceId = "android-" + Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
val session = GameSessionConfig.createGuest(
    "wss://test.imnono.net/game-ws/ws",
    "https://test.imnono.net/game-lobby",
    deviceId,
    2,              // 场景类型：1 直播间，2 IM 群
    sceneId,        // 直播间编号或群号
    0,
    BuildConfig.VERSION_NAME,
    "Android",
)
CocosGameSDK.getInstance().openGame(this, tableContainer, "niuniu", session)

override fun onGameEvent(gameId: String, event: String, payloadJson: String) {
    if (event == GameEvents.LOGIN_REQUIRED) {
        showLoginPrompt()
    }
}
```

示例见 Demo 首页「游客观战（直播间）」按钮与 `DemoEmbedActivity`。

## 17. 运行独立 Demo

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

## 18. 常见问题

### 18.1 Gradle 无法下载 SDK

检查是否能访问：

```text
https://raw.githubusercontent.com
```

清理后重试：

```shell
./gradlew --refresh-dependencies :app:assembleDebug
```

### 18.2 x86 模拟器无法运行

当前 SDK 只包含 `arm64-v8a`。使用 ARM 模拟器或 ARM64 真机。

### 18.3 sessionConfig 不能为空

UID、Token、Scene Type、Scene ID 必须由客户端登录和当前业务场景提供。

### 18.4 WebSocket 认证失败

检查 UID 与 Token 是否匹配、是否过期，以及 HTTP/WS 是否属于同一环境。

### 18.5 场景没有牛牛桌

检查 Scene Type、Scene ID，以及后台是否已在该场景开设牛牛桌。

## 19. 安全注意事项

- 不要将 UID、Token、账号密码提交到 Git。
- 不要在日志中打印 Token。
- 正式环境只使用 HTTPS/WSS。
- Token 失效后返回客户端登录流程。
- 不要使用动态版本号，例如 `game-sdk:+`。
- 生产环境固定 SDK 版本，并在升级前完成回归。
