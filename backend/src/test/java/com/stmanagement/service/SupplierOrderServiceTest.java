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

    // createJapaneseFont 全フォント戦略カバレッジ
    @Test void testExportPdf_allFontStrategies() throws Exception {
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        for (int i = 0; i < 3; i++) {
            byte[] pdf = service.exportPdf(1L);
            assertNotNull(pdf); assertTrue(pdf.length > 500);
        }
    }

    @Test void testExportPdf_withNullOptionalFields() throws Exception {
        // Null issuer/supplier fields force branches in PDF generation
        SupplierOrder so = SupplierOrder.builder().id(2L).orderNumber("PO-SUP-2026-0099")
            .supplierName("最小テスト").orderDate(java.time.LocalDate.now())
            .amount(100000.0).taxRate(10.0).taxAmount(10000.0).totalWithTax(110000.0)
            .status("下書き").build();
        when(repo.findById(2L)).thenReturn(Optional.of(so));
        when(detailRepo.findByOrderId(2L)).thenReturn(Collections.emptyList());
        byte[] pdf = service.exportPdf(2L);
        assertNotNull(pdf); assertTrue(pdf.length > 300);
    }

    @Test void testExportPdf_withFullFieldsAndDetails() throws Exception {
        SupplierOrder so = SupplierOrder.builder().id(3L).orderNumber("PO-SUP-2026-0100")
            .supplierName("フルテスト株式会社").orderDate(java.time.LocalDate.of(2026,6,1))
            .deliveryDate(java.time.LocalDate.of(2026,9,30)).subject("フルフィールドテスト")
            .amount(9990000.0).taxRate(10.0).taxAmount(999000.0).totalWithTax(10989000.0)
            .status("発注済").remark("全項目備考")
            .issuerName("発注太郎").issuerDept("発注部").issuerTel("090-1111-2222")
            .supplierContact("受注花子").supplierDept("受注部").supplierTel("03-9999-8888")
            .supplierAddr("東京都千代田区丸の内1-1-1").build();
        SupplierOrderDetail d = SupplierOrderDetail.builder().id(1L).orderId(3L)
            .employeeName("担当者A").itemName("設計作業").quantity(2.0).unitPrice(3000000.0).amount(6000000.0).build();
        when(repo.findById(3L)).thenReturn(Optional.of(so));
        when(detailRepo.findByOrderId(3L)).thenReturn(Collections.singletonList(d));
        byte[] pdf = service.exportPdf(3L);
        assertNotNull(pdf); assertTrue(pdf.length > 1500);
    }

    @Test void testCreate_withoutDeliveryDate() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("orderNumber", "TEST"); dto.put("supplierName", "テスト"); dto.put("orderDate", "2026-06-01");
        dto.put("amount", 1000000.0); dto.put("taxRate", 10.0);
        when(repo.save(any(SupplierOrder.class))).thenReturn(order);
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        Map<String, Object> r = service.create(dto);
        assertNotNull(r);
    }

    @Test void testCreate_withZeroAmount() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("orderNumber", "TEST2"); dto.put("supplierName", "テスト"); dto.put("orderDate", "2026-06-01");
        dto.put("amount", 0); dto.put("taxRate", 0);
        when(repo.save(any(SupplierOrder.class))).thenReturn(order);
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        Map<String, Object> r = service.create(dto);
        assertNotNull(r);
    }

    @Test void testUpdate_withDetails() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("supplierName", "更新"); dto.put("orderDate", "2026-07-01"); dto.put("amount", 3000000.0);
        dto.put("taxRate", 10.0);
        List<Map<String,Object>> details = new ArrayList<>();
        Map<String,Object> det = new HashMap<>();
        det.put("employeeName", "担当"); det.put("itemName", "品目"); det.put("quantity", 2.0); det.put("unitPrice", 500000.0);
        details.add(det);
        dto.put("details", details);
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(repo.save(any(SupplierOrder.class))).thenReturn(order);
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        Map<String, Object> r = service.update(1L, dto);
        assertNotNull(r);
    }

    @Test void testExportPdf_withDetails() throws Exception {
        SupplierOrderDetail detail = SupplierOrderDetail.builder().id(1L).orderId(1L)
                .employeeName("山田").itemName("テスト品目").quantity(2.0).unitPrice(500000.0).amount(1000000.0).build();
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(detail));
        byte[] pdf = service.exportPdf(1L);
        assertNotNull(pdf); assertTrue(pdf.length > 1000);
    }

    @Test void testFindAll_empty() {
        when(repo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        Page<Map<String, Object>> r = service.findAll(0, 10);
        assertNotNull(r); assertEquals(0, r.getTotalElements());
    }
}
