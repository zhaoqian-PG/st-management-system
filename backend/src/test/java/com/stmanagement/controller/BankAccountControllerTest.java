package com.stmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stmanagement.dto.BankAccountDTO;
import com.stmanagement.service.BankAccountService;
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

@WebMvcTest(BankAccountController.class)
class BankAccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private BankAccountService service;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void testList() throws Exception {
        when(service.findAll(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/bank-accounts"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetById() throws Exception {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setId(1L); dto.setTorihikiNo("BK000001"); dto.setBankName("テスト銀行");
        when(service.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/bank-accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.torihikiNo").value("BK000001"));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/bank-accounts/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindByCustomer() throws Exception {
        when(service.findByCustomerId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/bank-accounts/customer/1"))
                .andExpect(status().isOk());
    }
}
