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
import static org.mockito.Mockito.*;
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

    @Test void testFindByCustomer() throws Exception {
        when(service.findByCustomerId(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/bank-accounts/customer/1")).andExpect(status().isOk());
    }
    @Test void testFindByEmployee() throws Exception {
        when(service.findByEmployeeId(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/bank-accounts/employee/1")).andExpect(status().isOk());
    }
    @Test void testNextBranch() throws Exception {
        when(service.nextBranchNo("BK000001")).thenReturn("002");
        mockMvc.perform(get("/api/bank-accounts/next-branch/BK000001")).andExpect(status().isOk());
    }
    @Test void testTorihikiNos() throws Exception {
        when(service.getExistingTorihikiNos("CUSTOMER")).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/bank-accounts/torihiki-nos/CUSTOMER")).andExpect(status().isOk());
    }
    @Test void testBindCustomer() throws Exception {
        doNothing().when(service).bindToCustomer(1L, 2L);
        mockMvc.perform(put("/api/bank-accounts/1/bind/2")).andExpect(status().isOk());
    }
    @Test void testUnbindFromCustomer() throws Exception {
        doNothing().when(service).unbindFromCustomer(1L);
        mockMvc.perform(put("/api/bank-accounts/1/unbind")).andExpect(status().isOk());
    }
    @Test void testBindEmployee() throws Exception {
        doNothing().when(service).bindToEmployee(1L, 2L);
        mockMvc.perform(put("/api/bank-accounts/1/bind-employee/2")).andExpect(status().isOk());
    }
    @Test void testUnbindEmployee() throws Exception {
        doNothing().when(service).unbindFromEmployee(1L);
        mockMvc.perform(put("/api/bank-accounts/1/unbind-employee")).andExpect(status().isOk());
    }
    @Test void testSetDefaultCustomer() throws Exception {
        doNothing().when(service).setDefaultForCustomer(1L, 2L);
        mockMvc.perform(put("/api/bank-accounts/1/set-default-customer/2")).andExpect(status().isOk());
    }
    @Test void testSetDefaultEmployee() throws Exception {
        doNothing().when(service).setDefaultForEmployee(1L, 2L);
        mockMvc.perform(put("/api/bank-accounts/1/set-default-employee/2")).andExpect(status().isOk());
    }
    @Test void testUnlinkedByTorihiki() throws Exception {
        when(service.findUnlinkedByTorihikiNo("CUSTOMER","BK000001")).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/bank-accounts/unlinked/CUSTOMER/BK000001")).andExpect(status().isOk());
    }
}
