package com.stmanagement.integration;

import com.stmanagement.dto.AttendanceDTO;
import com.stmanagement.service.AttendanceService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AttendanceFindAllIT {

    @Autowired private AttendanceService att;

    @Test @Order(1) void findAll_allNull_bothFalse() {
        Page<AttendanceDTO> r = att.findAll(null, null, null, 0, 10);
        assertNotNull(r);
    }

    @Test @Order(2) void findAll_employeeIdOnly_employeeTrue_yearFalse() {
        Page<AttendanceDTO> r = att.findAll(null, null, 1L, 0, 10);
        assertNotNull(r);
    }

    @Test @Order(3) void findAll_yearMonthOnly_employeeFalse_yearTrue() {
        Page<AttendanceDTO> r = att.findAll(2026, 5, null, 0, 10);
        assertNotNull(r);
    }

    @Test @Order(4) void findAll_allThree_bothTrue() {
        // employeeId!=null AND year!=null&&month!=null → BOTH true
        Page<AttendanceDTO> r = att.findAll(2026, 5, 1L, 0, 10);
        assertNotNull(r);
    }

    @Test @Order(5) void findAll_yearOnly_monthNull() {
        Page<AttendanceDTO> r = att.findAll(2026, null, null, 0, 10);
        assertNotNull(r);
    }

    @Test @Order(6) void findAll_monthOnly_yearNull() {
        Page<AttendanceDTO> r = att.findAll(null, 5, null, 0, 10);
        assertNotNull(r);
    }
}
