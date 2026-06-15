package com.stmanagement.service;

import com.stmanagement.dto.*;
import com.stmanagement.model.*;
import com.stmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinalServiceCoverageTest {
    // PurchaseOrder
    @Mock private PurchaseOrderRepository poRepo;
    @Mock private PurchaseOrderDetailRepository poDetRepo;
    @Mock private PurchaseOrderAttachmentRepository poAttRepo;
    @Mock private CustomerRepository custRepo;
    @InjectMocks private PurchaseOrderService poService;

    // Employee
    @Mock private EmployeeRepository empRepo;
    @Mock private EmployeeAttachmentRepository empAttRepo;
    @Mock private BankAccountRepository baRepo;
    @Mock private javax.persistence.EntityManager em;
    @InjectMocks private EmployeeService empService;

    // SupplierOrder
    @Mock private SupplierOrderRepository soRepo;
    @Mock private SupplierOrderDetailRepository soDetRepo;
    @InjectMocks private SupplierOrderService soService;

    @TempDir Path tempDir;
    private PurchaseOrder po;
    private Employee emp;
    private Customer cust;

    @BeforeEach void setUp() {
        cust = new Customer(); cust.setId(1L); cust.setCompanyName("テスト");
        po = PurchaseOrder.builder().id(1L).orderNumber("PO-2026-0001").customerId(1L)
            .orderDate(LocalDate.of(2026,5,1)).deliveryDate(LocalDate.of(2026,6,15))
            .subject("テスト").amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0)
            .status("下書き").build();
        emp = new Employee(); emp.setId(1L); emp.setEmployeeCode("EMP0001"); emp.setName("山田");
        emp.setDepartment("営業部"); emp.setEmail("t@t.com"); emp.setStatus("在職");
        emp.setJoinDate(LocalDate.of(2020,4,1));
    }

    // PO: CSV with details
    @Test void po_exportCsv_withDetails() {
        when(poRepo.findById(1L)).thenReturn(Optional.of(po));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDetail d = PurchaseOrderDetail.builder().id(1L).orderId(1L)
            .employeeName("山田").itemName("サーバー").quantity(2.0).unitPrice(500000.0).amount(1000000.0).build();
        when(poDetRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(d));
        String csv = poService.exportOrderCsv(1L);
        assertNotNull(csv); assertTrue(csv.contains("サーバー")); assertTrue(csv.contains("山田"));
    }

    // PO: findById with attachments
    @Test void po_findById_withAttachments() {
        when(poRepo.findById(1L)).thenReturn(Optional.of(po));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        when(poDetRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        PurchaseOrderAttachment att = PurchaseOrderAttachment.builder().id(1L).orderId(1L)
            .fileName("添付.pdf").filePath("up/test.pdf").fileSize(1024L).build();
        when(poAttRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(att));
        PurchaseOrderDTO r = poService.findById(1L);
        assertEquals(1, r.getAttachments().size());
        assertEquals("添付.pdf", r.getAttachments().get(0).getFileName());
    }

    // PO: create with all fields
    @Test void po_create_allFields() {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder().customerId(1L)
            .orderDate(LocalDate.now()).deliveryDate(LocalDate.now().plusMonths(1))
            .subject("全項目").amount(2000000.0).taxRate(10.0)
            .issuerName("発注者").issuerDept("発注部").issuerTel("090-1111")
            .recipientName("受注者").recipientDept("受注部").recipientAddr("東京").recipientTel("03-2222")
            .remark("備考").build();
        when(poRepo.save(any())).thenReturn(po);
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        PurchaseOrderDTO r = poService.create(dto);
        assertNotNull(r);
    }

    // PO: delete with all
    @Test void po_delete() {
        doNothing().when(poDetRepo).deleteByOrderId(1L);
        doNothing().when(poAttRepo).deleteByOrderId(1L);
        doNothing().when(poRepo).deleteById(1L);
        poService.delete(1L);
        verify(poDetRepo).deleteByOrderId(1L);
        verify(poAttRepo).deleteByOrderId(1L);
        verify(poRepo).deleteById(1L);
    }

    // Employee: upload single file
    @Test void emp_uploadSingle() throws Exception {
        when(empRepo.findById(1L)).thenReturn(Optional.of(emp));
        MockMultipartFile f = new MockMultipartFile("f","doc.pdf","application/pdf","abc".getBytes());
        EmployeeAttachment att = new EmployeeAttachment(); att.setFileName("doc.pdf"); att.setFileSize(3L);
        when(empAttRepo.save(any())).thenReturn(att);
        List<EmployeeDTO.AttachmentInfo> r = empService.uploadFiles(1L, new org.springframework.web.multipart.MultipartFile[]{f});
        assertEquals(1, r.size());
    }

    // Employee: batch import multi-line
    @Test void emp_batchImport_twoLines() throws Exception {
        String csv = "EMP0401,花子,総務部,h@t.com\nEMP0402,健太,経理部,k@t.com";
        MockMultipartFile f = new MockMultipartFile("f","i.csv","text/csv",csv.getBytes("UTF-8"));
        when(empRepo.findAll()).thenReturn(Collections.emptyList());
        when(empRepo.save(any())).thenReturn(emp);
        assertEquals(2, empService.batchImport(f));
    }

    // Employee: export CSV
    @Test void emp_exportCsv() {
        when(empRepo.findAll()).thenReturn(Collections.singletonList(emp));
        String csv = empService.exportCsv();
        assertNotNull(csv); assertTrue(csv.contains("EMP0001"));
    }

    // Employee: findAll with keyword
    @Test void emp_findAll_keyword() {
        when(empRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        Page<EmployeeDTO> r = empService.findAll("山田", "営業部", false, 0, 10);
        assertEquals(1, r.getTotalElements());
    }

    // SupplierOrder: findById with details
    @Test void so_findById_withDetails() {
        SupplierOrder so = SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001")
            .supplierName("テスト").orderDate(LocalDate.of(2026,5,10)).amount(1000000.0)
            .taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0).status("下書き").build();
        when(soRepo.findById(1L)).thenReturn(Optional.of(so));
        SupplierOrderDetail d = SupplierOrderDetail.builder().id(1L).orderId(1L)
            .employeeName("山田").itemName("設計").quantity(1.0).unitPrice(500000.0).amount(500000.0).build();
        when(soDetRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(d));
        Map<String,Object> r = soService.findById(1L);
        assertNotNull(r); assertNotNull(r.get("details"));
    }

    // SupplierOrder: delete
    @Test void so_delete() {
        doNothing().when(soDetRepo).deleteByOrderId(1L);
        doNothing().when(soRepo).deleteById(1L);
        soService.delete(1L);
        verify(soDetRepo).deleteByOrderId(1L);
        verify(soRepo).deleteById(1L);
    }
}
