package com.stmanagement.service;

import com.stmanagement.dto.InvoiceDTO;
import com.stmanagement.dto.OrderDocumentDTO;
import com.stmanagement.model.*;
import com.stmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
class InvoiceFullCoverageTest {
    @Mock private InvoiceRepository invRepo;
    @Mock private InvoiceDetailRepository detRepo;
    @Mock private OrderDocumentRepository docRepo;
    @Mock private CustomerRepository custRepo;
    @Mock private EmployeeRepository empRepo;
    @Mock private BankAccountRepository baRepo;
    @InjectMocks private InvoiceService service;

    private Invoice inv;
    private Customer cust;

    @BeforeEach void setUp() {
        cust = new Customer(); cust.setId(1L); cust.setCompanyName("テスト"); cust.setAddress("東京"); cust.setPhone("03-1111");
        inv = new Invoice(); inv.setId(1L); inv.setInvoiceNumber("INV-2026-0501"); inv.setCustomerId(1L);
        inv.setYear(2026); inv.setMonth(5); inv.setAmount(1000000.0); inv.setTaxRate(10.0);
        inv.setTaxAmount(100000.0); inv.setTotalWithTax(1100000.0); inv.setStatus("下書き");
        inv.setInvoiceDate(LocalDate.of(2026,5,1)); inv.setDueDate(LocalDate.of(2026,6,30));
    }

    @Test void uploadDocument_works() throws Exception {
        when(invRepo.existsById(1L)).thenReturn(true);
        MockMultipartFile f = new MockMultipartFile("f","doc.pdf","application/pdf","data".getBytes());
        OrderDocument doc = new OrderDocument(); doc.setId(1L); doc.setFileName("doc.pdf");
        doc.setFilePath("upload/test.pdf"); doc.setFileSize(4L);
        when(docRepo.save(any())).thenReturn(doc);
        OrderDocumentDTO r = service.uploadDocument(1L, f);
        assertNotNull(r); assertEquals("doc.pdf", r.getFileName());
    }

    @Test void uploadDocument_notFound() {
        when(invRepo.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () ->
            service.uploadDocument(99L, new MockMultipartFile("f","a.pdf","application/pdf","x".getBytes())));
    }

    @Test void deleteDocument_works() {
        OrderDocument doc = new OrderDocument();
        doc.setId(1L); doc.setInvoiceId(1L); doc.setFileName("d.pdf"); doc.setFilePath("up/d.pdf");
        when(docRepo.findById(1L)).thenReturn(Optional.of(doc));
        doNothing().when(docRepo).deleteById(1L);
        service.deleteDocument(1L);
        verify(docRepo).deleteById(1L);
    }

    @Test void getDocumentFileName_works() {
        OrderDocument doc = new OrderDocument(); doc.setId(1L); doc.setFileName("myDoc.pdf");
        when(docRepo.findById(1L)).thenReturn(Optional.of(doc));
        assertEquals("myDoc.pdf", service.getDocumentFileName(1L));
    }

    @Test void exportCsv_withBankAccount() {
        when(invRepo.findById(1L)).thenReturn(Optional.of(inv));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        when(detRepo.findByInvoiceId(1L)).thenReturn(Collections.emptyList());
        BankAccount ba = new BankAccount(); ba.setBankName("テスト銀行"); ba.setBranchCode("038");
        ba.setAccountType("普通"); ba.setAccountNumber("123"); ba.setAccountHolder("名義"); ba.setIsDefault(true);
        when(baRepo.findByCustomerId(1L)).thenReturn(Collections.singletonList(ba));
        String csv = service.exportInvoiceCsv(1L);
        assertNotNull(csv); assertTrue(csv.contains("テスト銀行"));
    }

    @Test void findAll_withYearMonthCustomer() {
        when(invRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(inv)));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        Page<InvoiceDTO> r = service.findAll(2026, 5, 1L, 0, 10);
        assertEquals(1, r.getTotalElements());
    }

    @Test void findById_withDetailsAndDocs() {
        when(invRepo.findById(1L)).thenReturn(Optional.of(inv));
        when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        InvoiceDetail d = new InvoiceDetail(); d.setId(1L); d.setInvoiceId(1L); d.setEmployeeName("山田");
        d.setDescription("設計"); d.setQuantity(1.0); d.setUnitPrice(500000.0); d.setAmount(500000.0);
        when(detRepo.findByInvoiceId(1L)).thenReturn(Collections.singletonList(d));
        OrderDocument doc = new OrderDocument(); doc.setId(1L); doc.setFileName("添付.pdf");
        doc.setFilePath("up/a.pdf"); doc.setFileSize(100L);
        when(docRepo.findByInvoiceId(1L)).thenReturn(Collections.singletonList(doc));
        InvoiceDTO r = service.findById(1L);
        assertEquals(1, r.getDetails().size()); assertEquals(1, r.getDocuments().size());
    }

    @Test void delete_invoice() {
        when(invRepo.existsById(1L)).thenReturn(true);
        doNothing().when(invRepo).deleteById(1L);
        service.delete(1L);
        verify(invRepo).deleteById(1L);
    }
}
