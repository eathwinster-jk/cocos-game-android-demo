// 演示宿主在游戏下方放置自己的直播、聊天内容，接入方替换为真实页面即可。
package com.eath.cocosgamedemo;

import android.app.Activity;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eath.cocosgamesdk.CocosGameSDK;
import com.eath.cocosgamesdk.GameBottomContentProvider;

final class DemoBottomContentProvider implements GameBottomContentProvider {
    private static final int ACCENT_COLOR = Color.rgb(25, 180, 180);
    private static final int PADDING_HORIZONTAL_DP = 14;
    private static final int PADDING_VERTICAL_DP = 10;
    private static final int LINE_PADDING_DP = 6;

    /**
     * 创建示例聊天区：赛事信息、Tab 与几条聊天消息。
     */
    @Override
    public View createBottomContent(Activity activity, String gameId) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(activity, PADDING_HORIZONTAL_DP), dp(activity, PADDING_VERTICAL_DP),
            dp(activity, PADDING_HORIZONTAL_DP), dp(activity, PADDING_VERTICAL_DP));
        panel.addView(createText(activity, R.string.demo_bottom_match, 14, ACCENT_COLOR));
        panel.addView(createText(activity, R.string.demo_bottom_tabs, 17, Color.BLACK));
        panel.addView(createText(activity, R.string.demo_bottom_message_1, 15, Color.DKGRAY));
        panel.addView(createText(activity, R.string.demo_bottom_message_2, 15, Color.DKGRAY));
        panel.addView(createText(activity, R.string.demo_bottom_message_3, 15, ACCENT_COLOR));
        panel.addView(createMiniModeButton(activity));
        return panel;
    }

    /**
     * 演示宿主把牌桌缩小为迷你条，展开由游戏内按钮触发。
     */
    private View createMiniModeButton(Activity activity) {
        TextView button = createText(activity, R.string.demo_bottom_mini_mode, 15, ACCENT_COLOR);
        button.setOnClickListener(view -> CocosGameSDK.getInstance().setMiniMode(true));
        return button;
    }

    /**
     * 创建一行示例文字。
     */
    private TextView createText(Activity activity, int textRes, int sizeSp, int color) {
        TextView view = new TextView(activity);
        view.setText(textRes);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setPadding(0, dp(activity, LINE_PADDING_DP), 0, dp(activity, LINE_PADDING_DP));
        return view;
    }

    /**
     * dp 转像素。
     */
    private int dp(Activity activity, int value) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, activity.getResources().getDisplayMetrics()));
    }
}
