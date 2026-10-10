// 通过远程 Maven SDK 打开 Cocos 游戏的独立测试页面。
package com.eath.cocosgamedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eath.cocosgamesdk.CocosGameActivity;
import com.eath.cocosgamesdk.CocosGameSDK;
import com.eath.cocosgamesdk.GameSessionConfig;

public final class MainActivity extends Activity {
    private static final String LOG_TAG = "GameSdkConsumerDemo";
    private EditText userIdInput;
    private EditText tokenInput;
    private EditText sceneTypeInput;
    private EditText sceneIdInput;

    /** 初始化 SDK 和测试输入页。 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CocosGameSDK.getInstance().initialize(this);
        CocosGameSDK.getInstance().setHostListener((gameId, event, payloadJson) ->
            Log.i(LOG_TAG, "gameId=" + gameId + ", event=" + event + ", payload=" + payloadJson));
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
        userIdInput = createInput(R.string.user_id_hint, InputType.TYPE_CLASS_NUMBER);
        tokenInput = createInput(R.string.token_hint, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        sceneTypeInput = createInput(R.string.scene_type_hint, InputType.TYPE_CLASS_NUMBER);
        sceneIdInput = createInput(R.string.scene_id_hint, InputType.TYPE_CLASS_TEXT);
        root.addView(userIdInput, createItemParams(dp(56)));
        root.addView(tokenInput, createItemParams(dp(56)));
        root.addView(sceneTypeInput, createItemParams(dp(56)));
        root.addView(sceneIdInput, createItemParams(dp(56)));
        Button openButton = new Button(this);
        openButton.setText(R.string.open_game);
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
            String liveToken = tokenInput.getText().toString().trim();
            int sceneType = Integer.parseInt(sceneTypeInput.getText().toString().trim());
            String sceneId = sceneIdInput.getText().toString().trim();
            GameSessionConfig session = new GameSessionConfig(
                BuildConfig.GAME_WS_URL,
                BuildConfig.GAME_API_URL,
                liveUserId,
                liveToken,
                sceneType,
                sceneId,
                0,
                BuildConfig.VERSION_NAME,
                "Android");
            CocosGameSDK.getInstance().openGame(this, "niuniu", session);
        } catch (IllegalArgumentException error) {
            Log.e(LOG_TAG, "游戏参数校验失败", error);
            Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_LONG).show();
        }
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
