// Demo HTTP 公共工具，统一响应读取与查询参数编码。
package com.eath.cocosgamedemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class DemoHttpUtils {
    /**
     * 禁止实例化无状态工具类。
     */
    private DemoHttpUtils() {
    }

    /**
     * 读取成功或失败响应正文。
     */
    static String readResponse(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream input = statusCode >= 200 && statusCode < 300
            ? connection.getInputStream()
            : connection.getErrorStream();
        if (input == null) return "";
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        }
        return body.toString();
    }

    /**
     * 按 UTF-8 编码查询参数。
     */
    static String encodeQueryParameter(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }
}
