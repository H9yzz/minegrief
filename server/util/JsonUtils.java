package com.chebuya.minegriefserver.util;

import java.util.HashMap;
import java.util.Map;

public class JsonUtils {
    public static String mapToJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    public static Map<String, String> jsonToMap(String json) {
        Map<String, String> map = new HashMap<>();
        String content = json.substring(1, json.length() - 1);
        if (content.length() > 0) {
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                String key = keyValue[0].substring(1, keyValue[0].length() - 1);
                String value = keyValue[1].substring(1, keyValue[1].length() - 1);
                map.put(key, value);
            }
        }
        return map;
    }
}
