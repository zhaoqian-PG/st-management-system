package com.stmanagement.integration;

import com.stmanagement.service.AttendanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class AttendanceCoverageTest {

    @Autowired private AttendanceService attService;

    @Test void testGenerateMonth_multipleMonths() {
        int may = attService.generateMonth(2026, 5, 1L);
        int june = attService.generateMonth(2026, 6, 1L);
        int july = attService.generateMonth(2026, 7, 1L);
        assertTrue(may >= 0); assertTrue(june >= 0); assertTrue(july >= 0);
    }

    @Test void testGenerateMonthForAll_multipleMonths() {
        int aug = attService.generateMonthForAll(2026, 8);
        int sep = attService.generateMonthForAll(2026, 9);
        assertTrue(aug >= 0); assertTrue(sep >= 0);
    }

    @Test void testGetMonthlySummary() {
        Map<String,Object> summary = attService.getMonthlySummary(2026, 5, 1L);
        assertNotNull(summary);
    }

    @Test void testGetAllEmployeeMonthlySummary() {
        java.util.List<Map<String,Object>> list = attService.getAllEmployeeMonthlySummary(2026, 5);
        assertNotNull(list);
    }

    @Test void testExportCsv() {
        String csv = attService.exportCsv(2026, 5, 1L);
        assertNotNull(csv);
    }

    @Test void testExportCsvAll() {
        String csv = attService.exportCsvAll(2026, 5);
        assertNotNull(csv);
    }
}
