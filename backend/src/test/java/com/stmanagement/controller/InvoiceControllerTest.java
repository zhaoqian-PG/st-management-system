package com.stmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stmanagement.dto.InvoiceDTO;
import com.stmanagement.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private InvoiceService service;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void testList() throws Exception {
        InvoiceDTO dto = makeDTO();
        when(service.findAll(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(Collections.singletonList(dto)));

        mockMvc.perform(get("/api/invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].invoiceNumber").value("INV-2026-0501"));
    }

    @Test
    void testGetById() throws Exception {
        InvoiceDTO dto = makeDTO();
        when(service.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/invoice/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-2026-0501"));
    }

    @Test
    void testCreate() throws Exception {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L); dto.setYear(2026); dto.setMonth(6);
        dto.setAmount(500000.0);
        when(service.create(any())).thenReturn(makeDTO());

        mockMvc.perform(post("/api/invoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void testUpdate() throws Exception {
        InvoiceDTO dto = makeDTO();
        dto.setStatus("送付済");
        when(service.update(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/api/invoice/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test void testDelete() throws Exception {
        mockMvc.perform(delete("/api/invoice/1")).andExpect(status().isOk());
    }
    @Test void testFindById_full() throws Exception {
        InvoiceDTO dto = makeDTO();
        dto.setDocuments(Collections.emptyList());
        when(service.findById(1L)).thenReturn(dto);
        mockMvc.perform(get("/api/invoice/1")).andExpect(status().isOk());
    }
    @Test void testExport() throws Exception {
        when(service.exportInvoiceCsv(1L)).thenReturn("csv");
        mockMvc.perform(get("/api/invoice/export/1")).andExpect(status().isOk());
    }

    private InvoiceDTO makeDTO() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(1L); dto.setInvoiceNumber("INV-2026-0501"); dto.setCustomerId(1L);
        dto.setYear(2026); dto.setMonth(5); dto.setAmount(1500000.0);
        dto.setInvoiceDate(LocalDate.of(2026, 5, 1)); dto.setStatus("下書き");
        return dto;
    }
}
