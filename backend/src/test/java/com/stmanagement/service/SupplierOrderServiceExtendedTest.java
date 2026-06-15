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
class SupplierOrderServiceExtendedTest {
    @Mock private SupplierOrderRepository repo;
    @Mock private SupplierOrderDetailRepository detailRepo;
    @InjectMocks private SupplierOrderService service;

    private SupplierOrder order;
    @BeforeEach void setUp() {
        order = SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001")
            .supplierName("テスト").orderDate(LocalDate.of(2026,5,10)).deliveryDate(LocalDate.of(2026,7,1))
            .subject("テスト").amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0)
            .status("下書き").issuerName("山田").issuerDept("営業部").issuerTel("090-1111")
            .supplierContact("佐々木").supplierDept("開発部").supplierTel("03-1234").supplierAddr("東京都").build();
    }

    @Test void testCreate_withAllSupplierFields() {
        Map<String,Object> dto = new HashMap<>();
        dto.put("supplierName","A社"); dto.put("orderDate","2026-06-01"); dto.put("deliveryDate","2026-08-01");
        dto.put("amount",2000000.0); dto.put("taxRate",8.0); dto.put("subject","テスト");
        dto.put("issuerName","田中"); dto.put("issuerDept","技術部"); dto.put("issuerTel","090-9999");
        dto.put("supplierContact","鈴木"); dto.put("supplierDept","営業部");
        dto.put("supplierTel","06-1111"); dto.put("supplierAddr","大阪府");
        when(repo.save(any())).thenReturn(order);
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        Map<String,Object> r = service.create(dto);
        assertNotNull(r);
    }

    @Test void testUpdate_withSupplierFields() {
        Map<String,Object> dto = new HashMap<>();
        dto.put("supplierName","更新"); dto.put("orderDate","2026-07-01"); dto.put("amount",3000000.0); dto.put("taxRate",10.0);
        dto.put("supplierContact","高橋"); dto.put("supplierDept","開発部");
        dto.put("supplierTel","03-2222"); dto.put("supplierAddr","愛知県");
        dto.put("issuerName","佐藤"); dto.put("issuerDept","経理部"); dto.put("issuerTel","090-8888");
        dto.put("status","納品済");
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(repo.save(any())).thenReturn(order);
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        Map<String,Object> r = service.update(1L, dto);
        assertNotNull(r);
    }

    @Test void testFindById_withDetails() {
        SupplierOrderDetail d = SupplierOrderDetail.builder().id(1L).orderId(1L)
            .employeeName("山田").itemName("設計").quantity(1.0).unitPrice(500000.0).amount(500000.0).build();
        when(repo.findById(1L)).thenReturn(Optional.of(order));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(d));
        Map<String,Object> r = service.findById(1L);
        assertNotNull(r); assertNotNull(r.get("details"));
    }

    @Test void testExportPdf_withSupplierFields() throws Exception {
        SupplierOrder so = SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001")
            .supplierName("テスト").orderDate(LocalDate.of(2026,5,10)).deliveryDate(LocalDate.of(2026,7,1))
            .subject("テスト").amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0)
            .status("下書き").issuerName("山田").issuerDept("営業部").issuerTel("090-1111")
            .supplierContact("佐々木").supplierDept("開発部").supplierTel("03-1234").supplierAddr("東京都港区").build();
        SupplierOrderDetail d = SupplierOrderDetail.builder().id(1L).orderId(1L)
            .employeeName("山田").itemName("設計").quantity(1.0).unitPrice(500000.0).amount(500000.0).build();
        when(repo.findById(1L)).thenReturn(Optional.of(so));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(d));
        byte[] pdf = service.exportPdf(1L);
        assertNotNull(pdf); assertTrue(pdf.length > 2000);
    }

    @Test void testFindAll_multiplePages() {
        when(repo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(order)));
        Page<Map<String,Object>> r = service.findAll(0, 5);
        assertNotNull(r); assertEquals(1, r.getContent().size());
    }
}
