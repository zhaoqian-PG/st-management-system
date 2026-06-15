package com.stmanagement.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataFixRunnerTest {
    @Mock private JdbcTemplate jdbc;
    @InjectMocks private DataFixRunner runner;

    @Test void testRun_fixesNullBranchNo() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(jdbc.update(anyString())).thenReturn(1);
        runner.run();
        verify(jdbc, atLeastOnce()).update(contains("branch_no"));
        verify(jdbc, atLeastOnce()).update(contains("category"));
        verify(jdbc, atLeastOnce()).update(contains("is_default"));
        verify(jdbc, atLeastOnce()).update(contains("status"));
    }

    @Test void testRun_emptyPurchaseOrders() {
        when(jdbc.queryForObject(startsWith("SELECT COUNT(*) FROM purchase_order"), eq(Integer.class))).thenReturn(0);
        when(jdbc.queryForObject(startsWith("SELECT COUNT(*) FROM purchase_order_detail"), eq(Integer.class))).thenReturn(0);
        when(jdbc.update(anyString())).thenReturn(1);
        runner.run();
        verify(jdbc, atLeastOnce()).update(contains("INSERT INTO purchase_order"));
        verify(jdbc, atLeastOnce()).update(contains("INSERT INTO purchase_order_detail"));
    }
}
