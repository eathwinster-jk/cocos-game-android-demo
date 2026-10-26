// 演示宿主直播详情页嵌入牌桌：顶部容器放牌桌，下方是宿主自己的内容；须在 Manifest 声明 configChanges 才能横屏全屏。
package com.eath.cocosgamedemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.eath.cocosgamesdk.CocosGameSDK;
import com.eath.cocosgamesdk.GameHostListener;
import com.eath.cocosgamesdk.GameSessionConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

public final class DemoEmbedActivity extends Activity implements GameHostListener {
    private static final String TAG = "DemoEmbedActivity";
    private static final String EXTRA_SESSION_CONFIG = "sessionConfig";
    private static final String GAME_ID = "niuniu";
    private static final String WALLET_EVENT = "wallet.balance_changed";
    private static final float TABLE_HEIGHT_RATIO = 211F / 375F;
    private static final float MINI_BAR_HEIGHT_RATIO = 50F / 362F;
    private static final int MINI_BAR_MARGIN_DP = 6;
    private static final int ACCENT_COLOR = Color.rgb(25, 180, 180);
    private FrameLayout tableContainer;
    private TextView balanceView;

    /**
     * 创建打开嵌入页的 Intent。
     */
    static Intent createIntent(Activity activity, GameSessionConfig sessionConfig) {
        return new Intent(activity, DemoEmbedActivity.class).putExtra(EXTRA_SESSION_CONFIG, sessionConfig);
    }

    /**
     * 搭建宿主页面并把牌桌放入顶部容器。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Object sessionConfig = getIntent().getSerializableExtra(EXTRA_SESSION_CONFIG);
        if (!(sessionConfig instanceof GameSessionConfig)) {
            Log.e(TAG, "缺少大厅会话参数，关闭嵌入页");
            finish();
            return;
        }
        setContentView(createPage());
        CocosGameSDK.getInstance().setHostListener(this);
        CocosGameSDK.getInstance().openGame(this, tableContainer, GAME_ID, (GameSessionConfig) sessionConfig);
    }

    /**
     * 创建页面：牌桌容器 + 宿主内容。
     */
    private LinearLayout createPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.WHITE);
        tableContainer = new FrameLayout(this);
        page.addView(tableContainer, createTableParams(false));
        balanceView = createActionText(getString(R.string.demo_embed_balance, "-"));
        balanceView.setTextColor(Color.BLACK);
        page.addView(balanceView);
        page.addView(createActionRow());
        ScrollView hostContent = new ScrollView(this);
        hostContent.addView(new DemoBottomContentProvider().createBottomContent(this, GAME_ID));
        page.addView(hostContent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return page;
    }

    /**
     * 创建全屏与关闭按钮行。
     */
    private LinearLayout createActionRow() {
        LinearLayout row = new LinearLayout(this);
        TextView fullscreen = createActionText(getString(R.string.demo_embed_fullscreen));
        fullscreen.setOnClickListener(view -> CocosGameSDK.getInstance().toggleFullscreen());
        TextView close = createActionText(getString(R.string.demo_embed_close));
        close.setOnClickListener(view -> CocosGameSDK.getInstance().closeGame());
        row.addView(fullscreen);
        row.addView(close);
        return row;
    }

    /**
     * 创建一个文字按钮。
     */
    private TextView createActionText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(ACCENT_COLOR);
        view.setPadding(dp(14), dp(8), dp(14), dp(4));
        return view;
    }

    /**
     * 返回牌桌容器尺寸：正常 375:211，迷你条 362:50 且四周留边。
     */
    private LinearLayout.LayoutParams createTableParams(boolean isMiniMode) {
        int width = getResources().getDisplayMetrics().widthPixels;
        if (!isMiniMode) {
            return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(width * TABLE_HEIGHT_RATIO));
        }
        int margin = dp(MINI_BAR_MARGIN_DP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.round((width - margin * 2) * MINI_BAR_HEIGHT_RATIO));
        params.setMargins(margin, margin, margin, margin);
        return params;
    }

    @Override
    public void onGameEvent(String gameId, String event, String payloadJson) {
        if (!WALLET_EVENT.equals(event)) return;
        try {
            long balanceCent = new JSONObject(payloadJson).getLong("balanceCent");
            String amount = "¥" + BigDecimal.valueOf(balanceCent).movePointLeft(2).stripTrailingZeros().toPlainString();
            balanceView.setText(getString(R.string.demo_embed_balance, amount));
        } catch (JSONException error) {
            Log.e(TAG, "解析钱包事件失败，payload=" + payloadJson, error);
        }
    }

    @Override
    public void onMiniModeChanged(String gameId, boolean isMiniMode) {
        tableContainer.setLayoutParams(createTableParams(isMiniMode));
    }

    @Override
    public void onSessionInvalid(String gameId, String message) {
        Toast.makeText(this, getString(R.string.session_invalid, message), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGameClosed(String gameId) {
        Toast.makeText(this, R.string.demo_embed_closed, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * 返回键先交给牌桌：全屏退回竖屏、在座确认离座，未消费时再关闭页面。
     */
    @Override
    public void onBackPressed() {
        if (CocosGameSDK.getInstance().onBackPressed()) return;
        super.onBackPressed();
    }

    /**
     * 页面销毁时注销监听；牌桌由 SDK 随页面自动释放。
     */
    @Override
    protected void onDestroy() {
        CocosGameSDK.getInstance().setHostListener(null);
        super.onDestroy();
    }

    /**
     * dp 转像素。
     */
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
