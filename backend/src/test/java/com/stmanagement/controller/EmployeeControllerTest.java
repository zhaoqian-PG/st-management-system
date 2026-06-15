package com.stmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stmanagement.dto.EmployeeDTO;
import com.stmanagement.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private EmployeeService service;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void testList() throws Exception {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(1L); dto.setEmployeeCode("EMP0001"); dto.setName("山田 太郎");
        when(service.findAll(any(), any(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(Collections.singletonList(dto)));

        mockMvc.perform(get("/api/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("EMP0001"));
    }

    @Test
    void testGetById() throws Exception {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(1L); dto.setEmployeeCode("EMP0001"); dto.setName("山田 太郎");
        when(service.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/employee/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("山田 太郎"));
    }

    @Test
    void testCreate() throws Exception {
        EmployeeDTO input = new EmployeeDTO();
        input.setName("新規"); input.setDepartment("営業部");
        when(service.create(any())).thenReturn(new EmployeeDTO());

        mockMvc.perform(post("/api/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated());
    }

    @Test
    void testUpdate() throws Exception {
        EmployeeDTO input = new EmployeeDTO();
        input.setName("更新"); input.setDepartment("技術部");
        when(service.update(eq(1L), any())).thenReturn(new EmployeeDTO());

        mockMvc.perform(put("/api/employee/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());
    }

    @Test void testDelete() throws Exception {
        mockMvc.perform(delete("/api/employee/1")).andExpect(status().isOk());
    }
    @Test void testExport() throws Exception {
        when(service.exportCsv()).thenReturn("csv");
        mockMvc.perform(get("/api/employee/export")).andExpect(status().isOk());
    }
    @Test void testDeleteAttachment() throws Exception {
        mockMvc.perform(delete("/api/employee/attachments/1")).andExpect(status().isOk());
    }
}
