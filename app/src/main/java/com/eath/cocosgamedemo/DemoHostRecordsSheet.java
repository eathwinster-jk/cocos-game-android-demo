// 演示宿主自绘竖屏「记录与规则」：接收规则与记录 JSON，切换页签或加载更多时调用 SDK 查询。
package com.eath.cocosgamedemo;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.eath.cocosgamesdk.CocosGameSDK;
import com.eath.cocosgamesdk.GameRecordsListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class DemoHostRecordsSheet implements GameRecordsListener {
    private static final String TAG = "DemoHostRecordsSheet";
    private static final int ACCENT_COLOR = Color.rgb(25, 180, 180);
    private static final int TAB_CURRENT = 0;
    private static final int TAB_HISTORY = 1;
    private static final int TAB_RULES = 2;
    private static final float SHEET_HEIGHT_RATIO = 0.7F;
    private final Activity activity;
    private Dialog dialog;
    private LinearLayout tabRow;
    private LinearLayout content;
    private JSONObject rules;
    private JSONObject records;
    private int selectedTab = TAB_CURRENT;

    /**
     * 创建记录弹窗并注册为 SDK 记录监听。
     */
    DemoHostRecordsSheet(Activity activity) {
        this.activity = activity;
        CocosGameSDK.getInstance().setRecordsListener(this);
    }

    @Override
    public void onShowRecords(String gameId, String rulesJson) {
        try {
            rules = new JSONObject(rulesJson);
            records = rules.optJSONObject("records");
            selectedTab = TAB_CURRENT;
            ensureDialog().show();
            render();
        } catch (JSONException err) {
            Log.e(TAG, "解析规则数据失败，gameId=" + gameId, err);
        }
    }

    @Override
    public void onRecordsUpdated(String gameId, String recordsJson) {
        try {
            records = new JSONObject(recordsJson);
            if (dialog != null && dialog.isShowing()) render();
        } catch (JSONException err) {
            Log.e(TAG, "解析记录数据失败，gameId=" + gameId, err);
        }
    }

    /**
     * 页面销毁时注销监听并关闭弹窗。
     */
    void release() {
        CocosGameSDK.getInstance().setRecordsListener(null);
        if (dialog != null) dialog.dismiss();
    }

    /**
     * 创建底部弹窗：页签行 + 可滚动内容。
     */
    private Dialog ensureDialog() {
        if (dialog != null) return dialog;
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dp(14), dp(10), dp(14), dp(10));
        tabRow = new LinearLayout(activity);
        root.addView(tabRow);
        ScrollView scrollView = new ScrollView(activity);
        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1F));
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            int height = Math.round(activity.getResources().getDisplayMetrics().heightPixels * SHEET_HEIGHT_RATIO);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);
            window.setGravity(Gravity.BOTTOM);
        }
        return dialog;
    }

    /**
     * 按当前页签渲染。
     */
    private void render() {
        renderTabs();
        content.removeAllViews();
        if (selectedTab == TAB_RULES) renderRules();
        else if (records == null) addLine(activity.getString(R.string.demo_records_empty), 14, Color.GRAY);
        else renderRecords(selectedTab == TAB_HISTORY);
    }

    /**
     * 渲染页签：本桌记录、历史场次、规则。
     */
    private void renderTabs() {
        tabRow.removeAllViews();
        String currentTab = records == null ? "" : records.optString("currentTab");
        String historyTab = records == null ? "" : records.optString("historyTab");
        String[] labels = {currentTab, historyTab, rules.optString("title")};
        for (int index = 0; index < labels.length; index += 1) {
            int tab = index;
            TextView view = createText(labels[index], 16, index == selectedTab ? ACCENT_COLOR : Color.DKGRAY);
            view.setPadding(dp(4), dp(8), dp(16), dp(8));
            view.setOnClickListener(clicked -> selectTab(tab));
            tabRow.addView(view);
        }
    }

    /**
     * 切换页签，记录页签按默认条件重新查询。
     */
    private void selectTab(int tab) {
        selectedTab = tab;
        if (tab != TAB_RULES) {
            CocosGameSDK.getInstance().queryRecords(tab == TAB_HISTORY, "all", 0, "");
        }
        render();
    }

    /**
     * 渲染本桌记录或历史场次，以及加载状态。
     */
    private void renderRecords(boolean isHistory) {
        JSONObject section = records.optJSONObject(isHistory ? "history" : "current");
        if (section == null) return;
        addLine(section.optString("banner"), 13, Color.GRAY);
        JSONArray rows = section.optJSONArray(isHistory ? "sessions" : "items");
        int count = rows == null ? 0 : rows.length();
        for (int index = 0; index < count; index += 1) addRecordRow(rows.optJSONObject(index), isHistory);
        JSONObject list = section.optJSONObject("list");
        if (count == 0 && list != null && "ready".equals(list.optString("status"))) {
            addLine(records.optString("emptyText"), 14, Color.GRAY);
        }
        addFooter(list, isHistory);
    }

    /**
     * 渲染一行记录：本桌记录为 title/result/detail，历史场次为 title/amount/detail。
     */
    private void addRecordRow(JSONObject row, boolean isHistory) {
        if (row == null) return;
        String value = row.optString(isHistory ? "amount" : "result");
        addLine(row.optString("title") + "    " + value, 15, resolveToneColor(row.optString("tone")));
        addLine(row.optString("detail"), 13, Color.GRAY);
    }

    /**
     * 渲染列表底部：加载中、失败重试、加载更多或没有更多。
     */
    private void addFooter(JSONObject list, boolean isHistory) {
        JSONObject footer = records.optJSONObject("footer");
        if (list == null || footer == null) return;
        String status = list.optString("status");
        if ("loading".equals(status) || list.optBoolean("isLoadingMore")) {
            addLine(footer.optString("loading"), 14, Color.GRAY);
            return;
        }
        if ("error".equals(status)) {
            TextView retry = addLine(list.optString("message") + "  " + footer.optString("retry"), 14, ACCENT_COLOR);
            retry.setOnClickListener(view -> CocosGameSDK.getInstance().queryRecords(isHistory, "all", 0, ""));
            return;
        }
        if (!list.optBoolean("hasMore")) {
            addLine(footer.optString("noMore"), 13, Color.GRAY);
            return;
        }
        TextView loadMore = addLine(footer.optString("loadMore"), 14, ACCENT_COLOR);
        loadMore.setOnClickListener(view -> CocosGameSDK.getInstance().loadMoreRecords(isHistory));
    }

    /**
     * 渲染规则分组与条目。
     */
    private void renderRules() {
        JSONArray sections = rules.optJSONArray("sections");
        int sectionCount = sections == null ? 0 : sections.length();
        for (int index = 0; index < sectionCount; index += 1) {
            JSONObject section = sections.optJSONObject(index);
            if (section == null) continue;
            addLine(section.optString("title"), 16, Color.BLACK);
            JSONArray items = section.optJSONArray("items");
            int itemCount = items == null ? 0 : items.length();
            for (int itemIndex = 0; itemIndex < itemCount; itemIndex += 1) {
                JSONObject item = items.optJSONObject(itemIndex);
                if (item == null) continue;
                addLine(item.optString("title") + "  " + item.optString("value"), 14, Color.DKGRAY);
                addLine(item.optString("description"), 13, Color.GRAY);
            }
        }
    }

    /**
     * 输赢色：赢红、输绿、其他深灰。
     */
    private static int resolveToneColor(String tone) {
        if ("win".equals(tone)) return Color.rgb(230, 60, 60);
        if ("lose".equals(tone)) return Color.rgb(40, 160, 90);
        return Color.DKGRAY;
    }

    /**
     * 追加一行文字，空文案不追加。
     */
    private TextView addLine(String text, int sizeSp, int color) {
        TextView view = createText(text, sizeSp, color);
        if (!text.isEmpty()) content.addView(view);
        return view;
    }

    /**
     * 创建文字。
     */
    private TextView createText(String text, int sizeSp, int color) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setPadding(0, dp(3), 0, dp(3));
        return view;
    }

    /**
     * dp 转像素。
     */
    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, activity.getResources().getDisplayMetrics()));
    }
}
