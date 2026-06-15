package com.stmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stmanagement.dto.AttendanceDTO;
import com.stmanagement.service.AttendanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceController.class)
class AttendanceControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AttendanceService service;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void testList() throws Exception {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setId(1L); dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 1));
        dto.setWorkHours(8.0); dto.setStatus("出勤");
        when(service.findAll(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(Collections.singletonList(dto)));

        mockMvc.perform(get("/api/attendance"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetById() throws Exception {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setId(1L); dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 1));
        dto.setWorkHours(8.0); dto.setStatus("出勤");
        when(service.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/attendance/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workHours").value(8.0));
    }

    @Test
    void testCreate() throws Exception {
        AttendanceDTO input = new AttendanceDTO();
        input.setEmployeeId(1L); input.setWorkDate(LocalDate.of(2026, 5, 1));
        input.setWorkHours(8.0); input.setWorkType("NORMAL"); input.setStatus("出勤");

        AttendanceDTO saved = new AttendanceDTO();
        saved.setId(1L); saved.setEmployeeId(1L); saved.setStatus("出勤");
        when(service.create(any())).thenReturn(saved);

        mockMvc.perform(post("/api/attendance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated());
    }

    @Test
    void testUpdate() throws Exception {
        AttendanceDTO input = new AttendanceDTO();
        input.setEmployeeId(1L); input.setWorkDate(LocalDate.of(2026, 5, 2));
        input.setWorkHours(7.0); input.setStatus("出勤");
        when(service.update(eq(1L), any())).thenReturn(new AttendanceDTO());

        mockMvc.perform(put("/api/attendance/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/attendance/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testMonthlySummary() throws Exception {
        Map<String, Object> summary = new HashMap<>();
        summary.put("workHours", 160.0); summary.put("overtimeHours", 20.0);
        when(service.getMonthlySummary(anyInt(), anyInt(), anyLong())).thenReturn(summary);

        mockMvc.perform(get("/api/attendance/monthly-summary?year=2026&month=5&employeeId=1"))
                .andExpect(status().isOk());
    }
}
