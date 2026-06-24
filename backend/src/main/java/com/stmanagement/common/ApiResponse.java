package com.stmanagement.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified API response wrapper.
 * Usage: return ResponseEntity.ok(ApiResponse.success(data));
 */
public class ApiResponse {

    private ApiResponse() {
    }

    public static Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        response.put("message", "success");
        return response;
    }

    public static Map<String, Object> success(Object data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        response.put("message", message);
        return response;
    }

    public static Map<String, Object> error(String message, int status) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        response.put("status", status);
        return response;
    }
}
