package com.stmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stmanagement.dto.CustomerDTO;
import com.stmanagement.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CustomerService service;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void testList() throws Exception {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(1L); dto.setCustomerCode("CUS0001"); dto.setCompanyName("テスト株式会社");
        when(service.findAll(any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(Collections.singletonList(dto)));

        mockMvc.perform(get("/api/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].customerCode").value("CUS0001"));
    }

    @Test
    void testGetById() throws Exception {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(1L); dto.setCustomerCode("CUS0001"); dto.setCompanyName("テスト株式会社");
        when(service.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("テスト株式会社"));
    }

    @Test
    void testCreate() throws Exception {
        CustomerDTO input = new CustomerDTO();
        input.setCompanyName("新規"); input.setPresidentName("社長");
        when(service.create(any())).thenReturn(new CustomerDTO());

        mockMvc.perform(post("/api/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated());
    }

    @Test
    void testUpdate() throws Exception {
        CustomerDTO input = new CustomerDTO();
        input.setCompanyName("更新"); input.setCustomerCode("CUS0001");
        when(service.update(eq(1L), any())).thenReturn(new CustomerDTO());

        mockMvc.perform(put("/api/customer/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/customer/1"))
                .andExpect(status().isOk());
    }
}
