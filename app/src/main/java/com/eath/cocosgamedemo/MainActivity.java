// 通过远程 Maven SDK 打开 Cocos 游戏的独立测试页面。
package com.eath.cocosgamedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eath.cocosgamesdk.CocosGameActivity;
import com.eath.cocosgamesdk.CocosGameSDK;
import com.eath.cocosgamesdk.GameHostListener;
import com.eath.cocosgamesdk.GameSessionConfig;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity {
    private static final String LOG_TAG = "GameSdkConsumerDemo";
    private final AccountLoginClient accountLoginClient = new AccountLoginClient();
    private final ThreadPoolExecutor networkExecutor = new ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    private EditText accountInput;
    private EditText passwordInput;
    private EditText userIdInput;
    private EditText imUserIdInput;
    private EditText tokenInput;
    private EditText sceneTypeInput;
    private EditText sceneIdInput;
    private CheckBox embedModeCheckBox;
    private final GameHostListener hostListener = (gameId, event, payloadJson) ->
        Log.i(LOG_TAG, "gameId=" + gameId + ", event=" + event + ", payload=" + payloadJson);

    /** 初始化 SDK 和测试输入页。 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CocosGameSDK.getInstance().initialize(this);
        CocosGameSDK.getInstance().setBottomContentProvider(new DemoBottomContentProvider());
        setContentView(createContentView());
    }

    /** 创建 UID、Token 和场景输入页面。 */
    private LinearLayout createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(40), dp(24), dp(24));
        TextView title = new TextView(this);
        title.setText(R.string.title);
        title.setTextSize(24);
        root.addView(title, createItemParams(dp(64)));
        accountInput = createInput(R.string.account_hint, InputType.TYPE_CLASS_TEXT);
        passwordInput = createInput(
            R.string.password_hint,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        userIdInput = createInput(R.string.user_id_hint, InputType.TYPE_CLASS_NUMBER);
        imUserIdInput = createInput(R.string.im_user_id_hint, InputType.TYPE_CLASS_NUMBER);
        tokenInput = createInput(R.string.token_hint, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        sceneTypeInput = createInput(R.string.scene_type_hint, InputType.TYPE_CLASS_NUMBER);
        sceneIdInput = createInput(R.string.scene_id_hint, InputType.TYPE_CLASS_TEXT);
        sceneTypeInput.setText("2");
        sceneIdInput.setText("0999000015");
        root.addView(accountInput, createItemParams(dp(56)));
        root.addView(passwordInput, createItemParams(dp(56)));
        root.addView(sceneTypeInput, createItemParams(dp(56)));
        root.addView(sceneIdInput, createItemParams(dp(56)));
        embedModeCheckBox = new CheckBox(this);
        embedModeCheckBox.setText(R.string.embed_mode);
        root.addView(embedModeCheckBox, createItemParams(dp(48)));
        Button accountLoginButton = new Button(this);
        accountLoginButton.setText(R.string.account_login);
        accountLoginButton.setAllCaps(false);
        accountLoginButton.setOnClickListener(view -> handleAccountLogin());
        root.addView(accountLoginButton, createItemParams(dp(56)));
        root.addView(userIdInput, createItemParams(dp(56)));
        root.addView(imUserIdInput, createItemParams(dp(56)));
        root.addView(tokenInput, createItemParams(dp(56)));
        Button openButton = new Button(this);
        openButton.setText(R.string.open_with_token);
        openButton.setAllCaps(false);
        openButton.setOnClickListener(view -> openGame());
        root.addView(openButton, createItemParams(dp(56)));
        return root;
    }

    /** 创建单行输入框。 */
    private EditText createInput(int hintResourceId, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hintResourceId);
        input.setInputType(inputType);
        input.setSingleLine(true);
        return input;
    }

    /** 使用宿主输入的 UID 和 Token 打开游戏。 */
    private void openGame() {
        try {
            long liveUserId = Long.parseLong(userIdInput.getText().toString().trim());
            long imUserId = Long.parseLong(imUserIdInput.getText().toString().trim());
            String liveToken = tokenInput.getText().toString().trim();
            openGameWithSession(liveUserId, imUserId, liveToken);
        } catch (IllegalArgumentException error) {
            Log.e(LOG_TAG, "游戏参数校验失败", error);
            Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 使用账号密码换取 UID、Token 后打开游戏。
     */
    private void handleAccountLogin() {
        String account = accountInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (account.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.invalid_account, Toast.LENGTH_LONG).show();
            return;
        }
        networkExecutor.execute(() -> {
            try {
                AccountLoginClient.LoginResult result = accountLoginClient.login(account, password);
                runOnUiThread(() -> handleLoginResult(result));
            } catch (IOException | JSONException error) {
                Log.e(LOG_TAG, "账号登录网络异常", error);
                runOnUiThread(() ->
                    Toast.makeText(this, R.string.login_network_error, Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * 根据登录结果打开游戏或展示服务端错误。
     */
    private void handleLoginResult(AccountLoginClient.LoginResult result) {
        if (!result.isSuccessful()) {
            Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        try {
            openGameWithSession(result.getUserId(), result.getImUserId(), result.getToken());
        } catch (IllegalArgumentException error) {
            Log.e(LOG_TAG, "场景参数校验失败", error);
            Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 使用客户端会话和动态场景参数打开游戏。
     */
    private void openGameWithSession(long liveUserId, long imUserId, String liveToken) {
        int sceneType = Integer.parseInt(sceneTypeInput.getText().toString().trim());
        String sceneId = sceneIdInput.getText().toString().trim();
        GameSessionConfig session = new GameSessionConfig(
            BuildConfig.GAME_WS_URL,
            BuildConfig.GAME_API_URL,
            liveUserId,
            imUserId,
            liveToken,
            sceneType,
            sceneId,
            0,
            BuildConfig.VERSION_NAME,
            "Android");
        if (embedModeCheckBox.isChecked()) {
            startActivity(DemoEmbedActivity.createIntent(this, session));
            return;
        }
        CocosGameSDK.getInstance().openGame(this, "niuniu", session);
    }

    /** 回到本页时恢复监听；嵌入页关闭时会注销它自己的监听。 */
    @Override
    protected void onResume() {
        super.onResume();
        CocosGameSDK.getInstance().setHostListener(hostListener);
    }

    /** 处理游戏会话失效返回。 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CocosGameSDK.GAME_REQUEST_CODE || resultCode != RESULT_CANCELED) return;
        if (data == null || !data.hasExtra(CocosGameActivity.EXTRA_SESSION_ERROR)) return;
        String message = data.getStringExtra(CocosGameActivity.EXTRA_SESSION_ERROR);
        Toast.makeText(this, getString(R.string.session_invalid, message), Toast.LENGTH_LONG).show();
    }

    /** 页面销毁时释放宿主监听器。 */
    @Override
    protected void onDestroy() {
        CocosGameSDK.getInstance().setHostListener(null);
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    /** 创建统一纵向布局参数。 */
    private LinearLayout.LayoutParams createItemParams(int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height);
        params.topMargin = dp(12);
        return params;
    }

    /** 将 dp 转换为像素。 */
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
