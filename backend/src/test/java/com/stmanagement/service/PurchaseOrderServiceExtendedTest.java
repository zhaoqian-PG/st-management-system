package com.stmanagement.service;

import com.stmanagement.dto.PurchaseOrderDTO;
import com.stmanagement.model.*;
import com.stmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceExtendedTest {
    @Mock private PurchaseOrderRepository orderRepo;
    @Mock private PurchaseOrderDetailRepository detailRepo;
    @Mock private PurchaseOrderAttachmentRepository attRepo;
    @Mock private CustomerRepository custRepo;
    @InjectMocks private PurchaseOrderService service;

    private PurchaseOrder order;
    private Customer cust;
    @BeforeEach void setUp() {
        cust = new Customer(); cust.setId(1L); cust.setCompanyName("テスト株式会社");
        cust.setAddress("東京都"); cust.setPhone("03-1111-2222");
        order = PurchaseOrder.builder().id(1L).orderNumber("PO-2026-0001").customerId(1L)
            .orderDate(LocalDate.of(2026,5,1)).deliveryDate(LocalDate.of(2026,6,15))
            .subject("テスト").amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0)
            .status("下書き").remark("備考")
            .issuerName("山田").issuerDept("営業部").issuerTel("090-1111")
            .recipientName("田中").recipientDept("開発部").recipientAddr("大阪").recipientTel("06-9999")
            .attachmentPath("uploads/po_1_test.pdf").build();
    }

    @Test void testFindAll_withFilters() {
        when(orderRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(order)));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        Page<PurchaseOrderDTO> r = service.findAll(1L, "下書き", 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testFindById_fullDetails() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDetail det = PurchaseOrderDetail.builder().id(1L).orderId(1L)
            .employeeName("山田").itemName("サーバー").quantity(2.0).unitPrice(500000.0).amount(1000000.0).build();
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(det));
        PurchaseOrderAttachment att = PurchaseOrderAttachment.builder().id(1L).orderId(1L)
            .fileName("添付.pdf").filePath("uploads/test.pdf").fileSize(2048L).build();
        when(attRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(att));
        PurchaseOrderDTO r = service.findById(1L);
        assertNotNull(r); assertEquals(1, r.getDetails().size()); assertEquals(1, r.getAttachments().size());
    }
    @Test void testCreate_withAllFields() {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder().customerId(1L)
            .orderDate(LocalDate.of(2026,6,1)).deliveryDate(LocalDate.of(2026,8,1))
            .subject("フルテスト").amount(2000000.0).taxRate(10.0)
            .issuerName("佐藤").issuerDept("経理部").issuerTel("090-8888")
            .recipientName("鈴木").recipientDept("技術部").recipientAddr("愛知").recipientTel("052-1111")
            .remark("備考").build();
        when(orderRepo.save(any(PurchaseOrder.class))).thenReturn(order);
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDTO r = service.create(dto);
        assertNotNull(r);
    }
    @Test void testExportCsv_withDetails() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDetail det = PurchaseOrderDetail.builder().id(1L).orderId(1L)
            .employeeName("山田").itemName("サーバー").quantity(2.0).unitPrice(500000.0).amount(1000000.0).build();
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(det));
        String csv = service.exportOrderCsv(1L);
        assertNotNull(csv); assertTrue(csv.contains("サーバー"));
    }
    @Test void testFindAll_empty() {
        when(orderRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        Page<PurchaseOrderDTO> r = service.findAll(null, null, 0, 10);
        assertNotNull(r); assertTrue(r.isEmpty());
    }
}
