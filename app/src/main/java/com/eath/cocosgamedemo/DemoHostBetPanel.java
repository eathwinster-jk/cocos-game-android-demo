// 演示宿主自绘竖屏下注界面：接收下注选项、截止和结果回调，通过 submitBet 提交。
package com.eath.cocosgamedemo;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eath.cocosgamesdk.CocosGameSDK;
import com.eath.cocosgamesdk.GameBetListener;
import com.eath.cocosgamesdk.GameBetOptions;
import com.eath.cocosgamesdk.GameBetResult;

import java.util.List;
import java.util.Locale;

final class DemoHostBetPanel extends LinearLayout implements GameBetListener {
    private static final String TAG = "DemoHostBetPanel";
    private static final int ACCENT_COLOR = Color.rgb(25, 180, 180);
    private static final int CHOICE_COLOR = Color.rgb(240, 242, 245);
    private static final long COUNTDOWN_INTERVAL_MS = 1000L;
    private static final long CENTS_PER_YUAN = 100L;
    private final TextView titleView;
    private final LinearLayout areaRow;
    private final LinearLayout chipRow;
    private final TextView statusView;
    private final Runnable countdownTick = this::refreshTitle;
    private GameBetOptions options;
    private int selectedAreaIndex;

    /**
     * 创建下注面板并注册为 SDK 下注监听。
     */
    DemoHostBetPanel(Activity activity) {
        super(activity);
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setStroke(dp(1), ACCENT_COLOR);
        background.setCornerRadius(dp(12));
        setBackground(background);
        titleView = createText(15, Color.BLACK);
        areaRow = createRow();
        chipRow = createRow();
        statusView = createText(13, Color.DKGRAY);
        addView(titleView);
        addView(areaRow);
        addView(chipRow);
        addView(statusView);
        showWaiting();
        CocosGameSDK.getInstance().setBetListener(this);
    }

    @Override
    public void onBetOptions(String gameId, GameBetOptions betOptions) {
        options = betOptions;
        selectedAreaIndex = 0;
        bindChoices();
        statusView.setText("");
        refreshTitle();
    }

    @Override
    public void onBetClosed(String gameId, String roundId) {
        if (options == null || !options.getRoundId().equals(roundId)) return;
        options = null;
        removeCallbacks(countdownTick);
        areaRow.removeAllViews();
        chipRow.removeAllViews();
        titleView.setText(R.string.demo_bet_closed);
    }

    @Override
    public void onBetResult(String gameId, GameBetResult result) {
        if (result.isSuccess()) {
            Long balance = result.getBalanceCent();
            String balanceText = balance == null ? "-" : formatCent(balance);
            statusView.setText(getContext().getString(R.string.demo_bet_success,
                formatCent(result.getAmountCent()), balanceText));
            return;
        }
        statusView.setText(getContext().getString(R.string.demo_bet_failed, result.getErrorMessage()));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(countdownTick);
        CocosGameSDK.getInstance().setBetListener(null);
    }

    /**
     * 提交当前选中注区与筹码。
     */
    private void submit(long amountCent) {
        if (options == null) return;
        String areaCode = options.getAreas().get(selectedAreaIndex).getAreaCode();
        String clientBetNo = CocosGameSDK.createClientBetNo();
        try {
            boolean isSent = CocosGameSDK.getInstance().submitBet(options.getRoundId(), areaCode, amountCent, clientBetNo);
            statusView.setText(isSent ? R.string.demo_bet_submitting : R.string.demo_bet_rejected);
        } catch (IllegalArgumentException | IllegalStateException err) {
            Log.e(TAG, "提交下注失败，areaCode=" + areaCode + ", amountCent=" + amountCent, err);
            statusView.setText(getContext().getString(R.string.demo_bet_failed, err.getMessage()));
        }
    }

    /**
     * 按当前选项生成注区与筹码按钮。
     */
    private void bindChoices() {
        areaRow.removeAllViews();
        chipRow.removeAllViews();
        List<GameBetOptions.Area> areas = options.getAreas();
        for (int index = 0; index < areas.size(); index += 1) {
            int areaIndex = index;
            TextView button = createChoice(areas.get(index).getAreaName(), index == selectedAreaIndex);
            button.setOnClickListener(view -> selectArea(areaIndex));
            areaRow.addView(button, createChoiceParams());
        }
        for (Long chip : options.getChipsCent()) {
            TextView button = createChoice(formatCent(chip), false);
            button.setOnClickListener(view -> submit(chip));
            chipRow.addView(button, createChoiceParams());
        }
    }

    /**
     * 切换选中注区。
     */
    private void selectArea(int areaIndex) {
        selectedAreaIndex = areaIndex;
        bindChoices();
    }

    /**
     * 刷新局号与剩余秒数，每秒重算一次截止倒计时。
     */
    private void refreshTitle() {
        removeCallbacks(countdownTick);
        if (options == null) return;
        long remainMs = Math.max(0L, options.getDeadlineMillis() - System.currentTimeMillis());
        int remainSeconds = (int) ((remainMs + COUNTDOWN_INTERVAL_MS - 1) / COUNTDOWN_INTERVAL_MS);
        titleView.setText(getContext().getString(R.string.demo_bet_title, options.getRoundId(), remainSeconds));
        if (remainMs > 0) postDelayed(countdownTick, COUNTDOWN_INTERVAL_MS);
    }

    /**
     * 显示等待下一局下注。
     */
    private void showWaiting() {
        titleView.setText(R.string.demo_bet_waiting);
    }

    /**
     * 创建注区或筹码按钮。
     */
    private TextView createChoice(String text, boolean isSelected) {
        TextView button = createText(14, isSelected ? Color.WHITE : Color.BLACK);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable();
        background.setColor(isSelected ? ACCENT_COLOR : CHOICE_COLOR);
        background.setCornerRadius(dp(8));
        button.setBackground(background);
        return button;
    }

    /**
     * 创建等分按钮参数。
     */
    private LayoutParams createChoiceParams() {
        LayoutParams params = new LayoutParams(0, dp(36), 1F);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        return params;
    }

    /**
     * 创建横向按钮行。
     */
    private LinearLayout createRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        return row;
    }

    /**
     * 创建文字。
     */
    private TextView createText(int sizeSp, int color) {
        TextView view = new TextView(getContext());
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setPadding(0, dp(4), 0, dp(4));
        return view;
    }

    /**
     * 分转元展示。
     */
    private static String formatCent(long amountCent) {
        if (amountCent % CENTS_PER_YUAN == 0) return "¥" + amountCent / CENTS_PER_YUAN;
        return String.format(Locale.US, "¥%.2f", amountCent / (double) CENTS_PER_YUAN);
    }

    /**
     * dp 转像素。
     */
    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }
}
