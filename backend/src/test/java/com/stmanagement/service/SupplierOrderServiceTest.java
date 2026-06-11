package com.stmanagement.service;

import com.stmanagement.model.SupplierOrder;
import com.stmanagement.model.SupplierOrderDetail;
import com.stmanagement.repository.SupplierOrderDetailRepository;
import com.stmanagement.repository.SupplierOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierOrderServiceTest {

    @Mock private SupplierOrderRepository repo;
    @Mock private SupplierOrderDetailRepository detailRepo;
    @InjectMocks private SupplierOrderService service;

    private SupplierOrder order;

    @BeforeEach
    void setUp() {
        order = SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001")
                .supplierName("株式会社テスト").orderDate(LocalDate.of(2026, 5, 10))
                .deliveryDate(LocalDate.of(2026, 7, 1)).subject("サーバー構築")
                .amount(3500000.0).taxRate(10.0).taxAmount(350000.0).totalWithTax(3850000.0)
                .status("発注済").remark("テスト").issuerName("山田 太郎").issuerDept("営業部").issuerTel("090-1111-2222")
                .supplierContact("佐々木 健").supplierDept("開発部").supplierTel("03-1234-5678").supplierAddr("東京都港区")
                .build();
    }

    @Test
    void testFindAll() {
        List<SupplierOrder> orders = Collections.singletonList(order);
        Page<SupplierOrder> page = new PageImpl<>(orders);
        when(repo.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Map<String, Object>> result = service.findAll(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        Map<String, Object> m = result.getContent().get(0);
        assertEquals("PO-SUP-2026-0001", m.get("orderNumber"));
        assertEquals("株式会社テスト", m.get("supplierName"));
    }

    @Test
    void testFindById() {
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.findById(1L);

        assertNotNull(result);
        assertEquals("PO-SUP-2026-0001", result.get("orderNumber"));
        assertEquals("株式会社テスト", result.get("supplierName"));
        assertNotNull(result.get("details"));
    }

    @Test
    void testFindById_NotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }

    @Test
    void testCreate() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("orderNumber", "PO-SUP-2026-0004");
        dto.put("supplierName", "新規株式会社");
        dto.put("orderDate", "2026-06-01");
        dto.put("deliveryDate", "2026-08-01");
        dto.put("amount", 2000000.0);
        dto.put("taxRate", 10.0);
        dto.put("issuerName", "田中"); dto.put("issuerDept", "技術部"); dto.put("issuerTel", "090-9999-0000");
        dto.put("supplierContact", "鈴木"); dto.put("supplierDept", "営業部");
        dto.put("supplierTel", "03-0000-0000"); dto.put("supplierAddr", "大阪府");
        dto.put("subject", "テスト発注");
        dto.put("remark", "備考");

        when(repo.save(any(SupplierOrder.class))).thenReturn(order);
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.create(dto);

        assertNotNull(result);
        assertEquals("PO-SUP-2026-0001", result.get("orderNumber"));
        verify(repo).save(any(SupplierOrder.class));
    }

    @Test
    void testUpdate() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("supplierName", "更新株式会社");
        dto.put("orderDate", "2026-07-01");
        dto.put("deliveryDate", "2026-09-01");
        dto.put("amount", 3000000.0);
        dto.put("taxRate", 10.0);
        dto.put("issuerName", "佐藤"); dto.put("issuerDept", "経理部"); dto.put("issuerTel", "090-8888-7777");
        dto.put("supplierContact", "高橋"); dto.put("supplierDept", "開発部");
        dto.put("supplierTel", "06-1111-2222"); dto.put("supplierAddr", "愛知県");
        dto.put("subject", "更新発注");
        dto.put("status", "納品済");
        dto.put("remark", "更新備考");

        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(repo.save(any(SupplierOrder.class))).thenReturn(order);
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.update(1L, dto);

        assertNotNull(result);
        assertEquals("納品済", result.get("status"));
        verify(repo).save(any(SupplierOrder.class));
    }

    @Test
    void testDelete() {
        doNothing().when(detailRepo).deleteByOrderId(1L);
        doNothing().when(repo).deleteById(1L);

        service.delete(1L);

        verify(detailRepo).deleteByOrderId(1L);
        verify(repo).deleteById(1L);
    }

    @Test
    void testToMap_includesSupplierFields() {
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.findById(1L);

        assertEquals("佐々木 健", result.get("supplierContact"));
        assertEquals("開発部", result.get("supplierDept"));
        assertEquals("03-1234-5678", result.get("supplierTel"));
        assertEquals("東京都港区", result.get("supplierAddr"));
    }

    @Test
    void testToMap_includesIssuerFields() {
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.findById(1L);

        assertEquals("山田 太郎", result.get("issuerName"));
        assertEquals("営業部", result.get("issuerDept"));
        assertEquals("090-1111-2222", result.get("issuerTel"));
    }

    @Test
    void testExportPdf() throws Exception {
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());

        byte[] pdf = service.exportPdf(1L);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testExportPdf_NotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.exportPdf(99L));
    }
}
