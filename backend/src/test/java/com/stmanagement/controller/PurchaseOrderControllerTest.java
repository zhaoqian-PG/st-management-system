package com.stmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stmanagement.dto.PurchaseOrderAttachmentDTO;
import com.stmanagement.dto.PurchaseOrderDTO;
import com.stmanagement.dto.PurchaseOrderDetailDTO;
import com.stmanagement.service.PurchaseOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseOrderController.class)
class PurchaseOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PurchaseOrderService service;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void testList() throws Exception {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder().id(1L).orderNumber("PO-2026-0001")
                .customerId(1L).customerName("テスト").orderDate(LocalDate.of(2026, 5, 1))
                .amount(1000000.0).totalWithTax(1100000.0).status("下書き").build();
        when(service.findAll(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(Collections.singletonList(dto)));

        mockMvc.perform(get("/api/purchase-order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("PO-2026-0001"));
    }

    @Test
    void testGetById() throws Exception {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder().id(1L).orderNumber("PO-2026-0001")
                .customerId(1L).customerName("テスト").orderDate(LocalDate.of(2026, 5, 1))
                .amount(1000000.0).subject("テスト件名").remark("備考")
                .details(Collections.emptyList()).attachments(Collections.emptyList()).build();
        when(service.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/purchase-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("PO-2026-0001"))
                .andExpect(jsonPath("$.data.subject").value("テスト件名"))
                .andExpect(jsonPath("$.data.details").isArray())
                .andExpect(jsonPath("$.data.attachments").isArray());
    }

    @Test
    void testCreate() throws Exception {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder()
                .customerId(1L).orderDate(LocalDate.of(2026, 5, 1)).subject("新規").amount(1000000.0).build();
        PurchaseOrderDTO saved = PurchaseOrderDTO.builder().id(1L).orderNumber("PO-2026-0004")
                .customerId(1L).status("下書き").subject("新規").amount(1000000.0).build();
        when(service.create(any())).thenReturn(saved);

        mockMvc.perform(post("/api/purchase-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderNumber").value("PO-2026-0004"))
                .andExpect(jsonPath("$.message").value("注文書を登録しました"));
    }

    @Test
    void testUpdate() throws Exception {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder()
                .customerId(1L).orderDate(LocalDate.of(2026, 6, 1)).subject("更新").amount(2000000.0).status("発注済").build();
        PurchaseOrderDTO updated = PurchaseOrderDTO.builder().id(1L).orderNumber("PO-2026-0001")
                .customerId(1L).status("発注済").subject("更新").amount(2000000.0).build();
        when(service.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/purchase-order/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("発注済"))
                .andExpect(jsonPath("$.message").value("注文書を更新しました"));
    }

    @Test void testDelete() throws Exception {
        mockMvc.perform(delete("/api/purchase-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("注文書を削除しました"));
    }
    @Test void testDeleteAttachment() throws Exception {
        mockMvc.perform(delete("/api/purchase-order/attachment/1")).andExpect(status().isOk());
    }
    @Test void testDownloadAttachment() throws Exception {
        org.springframework.core.io.Resource res = new org.springframework.core.io.ByteArrayResource("test".getBytes());
        when(service.getAttachmentFile(1L)).thenReturn(res);
        when(service.getAttachmentFileName(1L)).thenReturn("test.pdf");
        mockMvc.perform(get("/api/purchase-order/attachment/1/download")).andExpect(status().isOk());
    }
}
