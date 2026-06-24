package com.stmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stmanagement.service.SupplierOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupplierOrderController.class)
class SupplierOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SupplierOrderService service;
    @Autowired private ObjectMapper objectMapper;

    private Map<String,Object> makeOrder(Long id, String orderNumber) {
        Map<String,Object> m = new HashMap<>();
        m.put("id",id); m.put("orderNumber",orderNumber); m.put("supplierName","テスト株式会社");
        m.put("orderDate","2026-05-10"); m.put("deliveryDate","2026-07-01");
        m.put("amount",1000000.0); m.put("taxRate",10.0); m.put("taxAmount",100000.0);
        m.put("totalWithTax",1100000.0); m.put("status","下書き"); m.put("subject","テスト");
        return m;
    }

    @Test
    void testList() throws Exception {
        Map<String,Object> o = makeOrder(1L, "PO-SUP-2026-0001");
        when(service.findAll(0, 10)).thenReturn(new PageImpl<>(Collections.singletonList(o)));

        mockMvc.perform(get("/api/supplier-order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("PO-SUP-2026-0001"));
    }

    @Test
    void testGetById() throws Exception {
        Map<String,Object> o = makeOrder(1L, "PO-SUP-2026-0001");
        when(service.findById(1L)).thenReturn(o);

        mockMvc.perform(get("/api/supplier-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("PO-SUP-2026-0001"));
    }

    @Test
    void testCreate() throws Exception {
        Map<String,Object> dto = new HashMap<>();
        dto.put("supplierName","テスト"); dto.put("orderDate","2026-05-10");
        dto.put("amount",1000000.0); dto.put("taxRate",10.0);
        when(service.create(any())).thenReturn(makeOrder(1L, "PO-SUP-2026-0004"));

        mockMvc.perform(post("/api/supplier-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("発注書を登録しました"));
    }

    @Test
    void testUpdate() throws Exception {
        Map<String,Object> dto = new HashMap<>();
        dto.put("supplierName","更新"); dto.put("orderDate","2026-06-01");
        dto.put("amount",2000000.0); dto.put("status","発注済");
        when(service.update(eq(1L), any())).thenReturn(makeOrder(1L, "PO-SUP-2026-0001"));

        mockMvc.perform(put("/api/supplier-order/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("発注書を更新しました"));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/supplier-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("発注書を削除しました"));
    }

    @Test
    void testExportPdf() throws Exception {
        when(service.exportPdf(1L)).thenReturn(new byte[]{1,2,3});

        mockMvc.perform(get("/api/supplier-order/export/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
