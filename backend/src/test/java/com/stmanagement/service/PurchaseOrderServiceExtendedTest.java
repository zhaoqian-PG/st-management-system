package com.stmanagement.service;

import com.stmanagement.dto.PurchaseOrderDTO;
import com.stmanagement.dto.PurchaseOrderDetailDTO;
import com.stmanagement.model.*;
import com.stmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private PurchaseOrderAttachment att;

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
        att = new PurchaseOrderAttachment(); att.setId(1L); att.setOrderId(1L);
        att.setFileName("test.pdf"); att.setFilePath("/tmp/test.pdf"); att.setFileSize(1024L);
    }

    // ─── findAll ───
    @Test void testFindAll_withFilters() {
        when(orderRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(order)));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        Page<PurchaseOrderDTO> r = service.findAll(1L, "下書き", 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testFindAll_empty() {
        when(orderRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        Page<PurchaseOrderDTO> r = service.findAll(null, null, 0, 10);
        assertNotNull(r); assertTrue(r.isEmpty());
    }

    // ─── findById ───
    @Test void testFindById_fullDetails() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDetail det = PurchaseOrderDetail.builder().id(1L).orderId(1L)
            .employeeName("山田").itemName("サーバー").quantity(2.0).unitPrice(500000.0).amount(1000000.0).build();
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(det));
        when(attRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(att));
        PurchaseOrderDTO r = service.findById(1L);
        assertNotNull(r); assertEquals(1, r.getDetails().size()); assertEquals(1, r.getAttachments().size());
    }

    // ─── create ───
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
    @Test void testCreate_withDetails() {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder().customerId(1L)
            .orderDate(LocalDate.of(2026,6,1)).amount(500000.0).taxRate(10.0).subject("test").build();
        PurchaseOrderDetailDTO detail = new PurchaseOrderDetailDTO();
        detail.setItemName("マウス"); detail.setQuantity(2.0); detail.setUnitPrice(5000.0);
        dto.setDetails(Collections.singletonList(detail));
        when(orderRepo.save(any(PurchaseOrder.class))).thenReturn(order);
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDTO r = service.create(dto);
        assertNotNull(r);
    }
    @Test void testCreate_nullAmountAndTaxRate() {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder().customerId(1L)
            .orderDate(LocalDate.of(2026,6,1)).subject("test").build();
        // amount=null, taxRate=null → toEntity defaults
        when(orderRepo.save(any(PurchaseOrder.class))).thenReturn(order);
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDTO r = service.create(dto);
        assertNotNull(r);
    }

    // ─── update ───
    @Test void testUpdate_NotFound() {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder().customerId(1L).subject("test").build();
        when(orderRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.update(999L, dto));
    }
    @Test void testUpdate_nullAmountAndTaxRate() {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder().customerId(1L).subject("test").build();
        // amount=null, taxRate=null
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepo.save(any(PurchaseOrder.class))).thenReturn(order);
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDTO r = service.update(1L, dto);
        assertNotNull(r);
    }
    @Test void testUpdate_withDetails() {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder().customerId(1L)
            .amount(1000000.0).taxRate(10.0).subject("test").build();
        PurchaseOrderDetailDTO detail = new PurchaseOrderDetailDTO();
        detail.setItemName("キーボード"); detail.setQuantity(1.0); detail.setUnitPrice(3000.0);
        dto.setDetails(Collections.singletonList(detail));
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepo.save(any(PurchaseOrder.class))).thenReturn(order);
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDTO r = service.update(1L, dto);
        assertNotNull(r);
        verify(detailRepo).deleteByOrderId(1L);
    }
    @Test void testUpdate_detailsNullQuantityUnitPrice() {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder().customerId(1L)
            .amount(1000000.0).taxRate(10.0).subject("test").build();
        PurchaseOrderDetailDTO detail = new PurchaseOrderDetailDTO();
        detail.setItemName("商品"); detail.setQuantity(null); detail.setUnitPrice(null);
        dto.setDetails(Collections.singletonList(detail));
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepo.save(any(PurchaseOrder.class))).thenReturn(order);
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDTO r = service.update(1L, dto);
        assertNotNull(r);
    }

    // ─── uploadAttachment ───
    @Test void testUploadAttachment_success() throws IOException {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepo.save(any(PurchaseOrder.class))).thenReturn(order);
        when(attRepo.save(any(PurchaseOrderAttachment.class))).thenReturn(att);
        MockMultipartFile file = new MockMultipartFile("file", "po_test.pdf",
                "application/pdf", "pdf content".getBytes());
        service.uploadAttachment(1L, file);
        verify(orderRepo, times(2)).findById(1L);
        verify(orderRepo).save(any(PurchaseOrder.class));
        verify(attRepo).save(any(PurchaseOrderAttachment.class));
    }

    @Test void testUploadAttachment_NotFound() {
        when(orderRepo.findById(999L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                "application/pdf", "data".getBytes());
        assertThrows(RuntimeException.class, () -> service.uploadAttachment(999L, file));
    }

    // ─── getAttachmentFile ───
    @Test void testGetAttachmentFile_success(@TempDir Path tempDir) throws IOException {
        Path realFile = tempDir.resolve("real.pdf");
        Files.write(realFile, "test".getBytes());
        PurchaseOrderAttachment realAtt = new PurchaseOrderAttachment();
        realAtt.setId(1L); realAtt.setFilePath(realFile.toString());
        when(attRepo.findById(1L)).thenReturn(Optional.of(realAtt));
        org.springframework.core.io.Resource r = service.getAttachmentFile(1L);
        assertNotNull(r); assertTrue(r.exists());
    }

    @Test void testGetAttachmentFile_fileNotOnDisk() {
        PurchaseOrderAttachment missing = new PurchaseOrderAttachment();
        missing.setId(1L); missing.setFilePath("/nonexistent/file.pdf");
        when(attRepo.findById(1L)).thenReturn(Optional.of(missing));
        assertThrows(RuntimeException.class, () -> service.getAttachmentFile(1L));
    }

    // ─── getAttachmentFileName ───
    @Test void testGetAttachmentFileName_notFound_returnsDownload() {
        when(attRepo.findById(999L)).thenReturn(Optional.empty());
        assertEquals("download", service.getAttachmentFileName(999L));
    }

    @Test void testGetAttachmentFileName_nullName() {
        PurchaseOrderAttachment nullName = new PurchaseOrderAttachment();
        nullName.setId(1L); nullName.setFileName(null);
        when(attRepo.findById(1L)).thenReturn(Optional.of(nullName));
        // map(nullFileName) → Optional.empty → orElse("download")
        assertEquals("download", service.getAttachmentFileName(1L));
    }

    // ─── deleteAttachment ───
    @Test void testDeleteAttachment_success() {
        when(attRepo.findById(1L)).thenReturn(Optional.of(att));
        doNothing().when(attRepo).delete(att);
        service.deleteAttachment(1L);
        verify(attRepo).delete(att);
    }

    // ─── exportOrderCsv ───
    @Test void testExportCsv_withDetails() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDetail det = PurchaseOrderDetail.builder().id(1L).orderId(1L)
            .employeeName("山田").itemName("サーバー").quantity(2.0).unitPrice(500000.0).amount(1000000.0).build();
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(det));
        String csv = service.exportOrderCsv(1L);
        assertNotNull(csv); assertTrue(csv.contains("サーバー"));
    }

    @Test void testExportCsv_customerNotFound() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(custRepo.findById(1L)).thenReturn(Optional.empty());
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        String csv = service.exportOrderCsv(1L);
        assertNotNull(csv); assertTrue(csv.contains("PO-2026-0001"), "CSVには注文番号が含まれるべき");
    }

    @Test void testExportCsv_nullOptionalFields() {
        order.setDeliveryDate(null); order.setRecipientDept(null); order.setRecipientName(null);
        order.setRecipientTel(null); order.setSubject(null);
        order.setTaxRate(null); order.setTaxAmount(null); order.setTotalWithTax(null);
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        String csv = service.exportOrderCsv(1L);
        assertNotNull(csv); assertTrue(csv.contains("PO-2026-0001"), "CSVには注文番号が含まれるべき");
    }

    @Test void testExportCsv_detailNullFields() {
        PurchaseOrderDetail detail = new PurchaseOrderDetail();
        detail.setId(1L); detail.setOrderId(1L);
        detail.setItemName("キーボード"); detail.setQuantity(null);
        detail.setUnitPrice(null); detail.setAmount(null); detail.setEmployeeName(null);
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(detail));
        String csv = service.exportOrderCsv(1L);
        assertNotNull(csv); assertTrue(csv.contains("キーボード"));
    }

    @Test void testExportCsv_NotFound() {
        when(orderRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.exportOrderCsv(999L));
    }

    // ─── toDTO: customer not found ───
    @Test void testFindById_customerNotFound() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(custRepo.findById(1L)).thenReturn(Optional.empty());
        when(detailRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        when(attRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        PurchaseOrderDTO r = service.findById(1L);
        assertNotNull(r); assertEquals("", r.getCustomerName());
    }
}
