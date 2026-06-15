package com.stmanagement.service;

import com.stmanagement.dto.InvoiceDTO;
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
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceExtendedTest {
    @Mock private InvoiceRepository invoiceRepo;
    @Mock private InvoiceDetailRepository detailRepo;
    @Mock private OrderDocumentRepository docRepo;
    @Mock private CustomerRepository custRepo;
    @Mock private EmployeeRepository empRepo;
    @Mock private BankAccountRepository baRepo;
    @InjectMocks private InvoiceService service;

    private Invoice inv;
    private Customer cust;
    @BeforeEach void setUp() {
        cust = new Customer(); cust.setId(1L); cust.setCompanyName("テスト株式会社");
        cust.setAddress("東京都"); cust.setPhone("03-1111"); cust.setContactName("担当者");
        inv = new Invoice(); inv.setId(1L); inv.setInvoiceNumber("INV-2026-0501"); inv.setCustomerId(1L);
        inv.setYear(2026); inv.setMonth(5); inv.setAmount(1500000.0);
        inv.setTaxRate(10.0); inv.setTaxAmount(150000.0); inv.setTotalWithTax(1650000.0);
        inv.setStatus("下書き"); inv.setSubject("テスト"); inv.setInvoiceDate(LocalDate.of(2026,5,1));
        inv.setDueDate(LocalDate.of(2026,6,30));
    }

    @Test void testFindAll_withAllFilters() {
        when(invoiceRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(inv)));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        Page<InvoiceDTO> r = service.findAll(2026, 5, 1L, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testFindById_fullDetails() {
        when(invoiceRepo.findById(1L)).thenReturn(Optional.of(inv));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        InvoiceDetail det = new InvoiceDetail(); det.setId(1L); det.setInvoiceId(1L);
        det.setEmployeeName("山田"); det.setDescription("基本設計"); det.setQuantity(1.0); det.setUnitPrice(500000.0); det.setAmount(500000.0);
        when(detailRepo.findByInvoiceId(1L)).thenReturn(Collections.singletonList(det));
        OrderDocument doc = new OrderDocument(); doc.setId(1L); doc.setInvoiceId(1L);
        doc.setFileName("注文書.pdf"); doc.setFilePath("inv/test.pdf"); doc.setFileSize(1024L);
        when(docRepo.findByInvoiceId(1L)).thenReturn(Collections.singletonList(doc));
        InvoiceDTO r = service.findById(1L);
        assertNotNull(r); assertEquals(1, r.getDetails().size()); assertEquals(1, r.getDocuments().size());
    }
    @Test void testCreate_withDetails() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L); dto.setYear(2026); dto.setMonth(6); dto.setAmount(500000.0); dto.setTaxRate(10.0);
        dto.setInvoiceDate(LocalDate.of(2026,6,1)); dto.setDueDate(LocalDate.of(2026,7,31));
        dto.setSubject("テスト請求"); dto.setRemark("備考");
        when(invoiceRepo.save(any(Invoice.class))).thenReturn(inv);
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        InvoiceDTO r = service.create(dto);
        assertNotNull(r);
    }
    @Test void testExportCsv_withBankInfo() {
        when(invoiceRepo.findById(1L)).thenReturn(Optional.of(inv));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        when(detailRepo.findByInvoiceId(1L)).thenReturn(Collections.emptyList());
        BankAccount ba = new BankAccount(); ba.setId(1L); ba.setBankName("三菱UFJ");
        ba.setBranchCode("038"); ba.setAccountType("普通"); ba.setAccountNumber("7654321");
        ba.setAccountHolder("テスト"); ba.setIsDefault(true);
        when(baRepo.findByCustomerId(1L)).thenReturn(Collections.singletonList(ba));
        String csv = service.exportInvoiceCsv(1L);
        assertNotNull(csv); assertTrue(csv.contains("三菱UFJ"));
    }
    @Test void testUploadDocument_success() throws Exception {
        when(invoiceRepo.existsById(1L)).thenReturn(true);
        MockMultipartFile file = new MockMultipartFile("file","test.pdf","application/pdf","data".getBytes());
        OrderDocument doc = new OrderDocument(); doc.setId(1L); doc.setFileName("test.pdf");
        when(docRepo.save(any(OrderDocument.class))).thenReturn(doc);
        assertNotNull(service.uploadDocument(1L, file));
    }
    @Test void testFindAll_empty() {
        when(invoiceRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        Page<InvoiceDTO> r = service.findAll(null, null, null, 0, 10);
        assertNotNull(r); assertTrue(r.isEmpty());
    }
}
