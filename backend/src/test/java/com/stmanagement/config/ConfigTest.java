package com.stmanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void testCorsConfig() {
        CorsConfig config = new CorsConfig();
        org.springframework.web.filter.CorsFilter filter = config.corsFilter();
        assertNotNull(filter);
    }

    @Test
    void testCorsConfiguration_allowsAll() {
        CorsConfiguration c = new CorsConfiguration();
        c.addAllowedOriginPattern("*");
        c.addAllowedMethod("*");
        c.addAllowedHeader("*");
        c.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);

        CorsConfiguration r = source.getCorsConfigurations().get("/**");
        assertNotNull(r);
        assertTrue(r.getAllowedOriginPatterns().contains("*"));
    }

    @Test
    void testGlobalExceptionHandler_RuntimeException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntimeException(new RuntimeException("テスト"));
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
    }

}
