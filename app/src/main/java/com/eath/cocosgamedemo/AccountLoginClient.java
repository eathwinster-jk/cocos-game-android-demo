// Demo 账号密码登录客户端，负责换取业务 UID 与 Token。
package com.eath.cocosgamedemo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

final class AccountLoginClient {
    private static final int TIMEOUT_MILLISECONDS = 20000;

    /**
     * 请求登录并返回业务会话。
     */
    LoginResult login(String account, String password) throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            connection = createConnection(account, password);
            int statusCode = connection.getResponseCode();
            String responseBody = DemoHttpUtils.readResponse(connection, statusCode);
            return parseResult(statusCode, responseBody);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * 创建账号登录连接。
     */
    private HttpURLConnection createConnection(String account, String password) throws IOException {
        String requestUrl = BuildConfig.LOGIN_API_URL
            + "?account=" + DemoHttpUtils.encodeQueryParameter(account)
            + "&password=" + DemoHttpUtils.encodeQueryParameter(password)
            + "&apptype=" + BuildConfig.LOGIN_APP_TYPE
            + "&pid=" + BuildConfig.LOGIN_PID
            + "&countryCode=" + DemoHttpUtils.encodeQueryParameter(BuildConfig.LOGIN_COUNTRY_CODE);
        HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(TIMEOUT_MILLISECONDS);
        connection.setRequestProperty("pid", String.valueOf(BuildConfig.LOGIN_PID));
        connection.setRequestProperty("version", BuildConfig.LOGIN_VERSION);
        connection.setRequestProperty("User-Agent", BuildConfig.LOGIN_USER_AGENT);
        return connection;
    }

    /**
     * 解析登录业务状态、UID 和 Token。
     */
    private LoginResult parseResult(int statusCode, String responseBody) throws JSONException {
        if (responseBody.isEmpty()) return LoginResult.failure("登录服务未返回有效响应");
        JSONObject response = new JSONObject(responseBody);
        String message = response.optString("msg", "登录失败");
        if (statusCode < 200 || statusCode >= 300) return LoginResult.failure(message);
        if (!"0".equals(response.optString("code", "-1"))) return LoginResult.failure(message);
        long userId = parseUserId(response.optString("uid", "0"));
        String token = response.optString("token", "");
        if (userId <= 0 || token.isEmpty()) return LoginResult.failure("登录响应缺少 UID 或 Token");
        return LoginResult.success(userId, token);
    }

    /**
     * 安全解析用户 ID。
     */
    private long parseUserId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException error) {
            return 0L;
        }
    }

    /**
     * 不可变登录结果。
     */
    static final class LoginResult {
        private final boolean isSuccessful;
        private final String message;
        private final long userId;
        private final String token;

        /**
         * 创建登录结果。
         */
        private LoginResult(boolean isSuccessful, String message, long userId, String token) {
            this.isSuccessful = isSuccessful;
            this.message = message;
            this.userId = userId;
            this.token = token;
        }

        /** 创建成功结果。 */
        static LoginResult success(long userId, String token) {
            return new LoginResult(true, "", userId, token);
        }

        /** 创建失败结果。 */
        static LoginResult failure(String message) {
            return new LoginResult(false, message, 0L, "");
        }

        /** 返回是否成功。 */
        boolean isSuccessful() { return isSuccessful; }

        /** 返回提示信息。 */
        String getMessage() { return message; }

        /** 返回直播用户 ID。 */
        long getUserId() { return userId; }

        /** 返回直播 Token。 */
        String getToken() { return token; }
    }
}
