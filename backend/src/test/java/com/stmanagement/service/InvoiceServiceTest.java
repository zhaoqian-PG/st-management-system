package com.stmanagement.service;

import com.stmanagement.dto.InvoiceDTO;
import com.stmanagement.dto.InvoiceDetailDTO;
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
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceDetailRepository detailRepository;
    @Mock private OrderDocumentRepository orderDocumentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @InjectMocks private InvoiceService service;

    private Invoice invoice;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer(); customer.setId(1L); customer.setCompanyName("テスト株式会社");
        customer.setAddress("東京都"); customer.setPhone("03-1111-2222");
        invoice = new Invoice(); invoice.setId(1L); invoice.setInvoiceNumber("INV-2026-0501"); invoice.setCustomerId(1L);
        invoice.setYear(2026); invoice.setMonth(5); invoice.setAmount(1500000.0);
        invoice.setTaxRate(10.0); invoice.setTaxAmount(150000.0); invoice.setTotalWithTax(1650000.0);
        invoice.setStatus("下書き"); invoice.setSubject("テスト"); invoice.setInvoiceDate(LocalDate.of(2026, 5, 1));
        invoice.setDueDate(LocalDate.of(2026, 6, 30));
    }

    @Test void testFindAll() {
        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(invoice)));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        Page<InvoiceDTO> r = service.findAll(null, null, null, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testFindById() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(detailRepository.findByInvoiceId(1L)).thenReturn(Collections.emptyList());
        when(orderDocumentRepository.findByInvoiceId(1L)).thenReturn(Collections.emptyList());
        InvoiceDTO r = service.findById(1L);
        assertNotNull(r); assertEquals("INV-2026-0501", r.getInvoiceNumber());
    }
    @Test void testFindById_NotFound() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }
    @Test void testCreate() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L); dto.setYear(2026); dto.setMonth(6); dto.setAmount(500000.0); dto.setTaxRate(10.0);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        assertNotNull(service.create(dto));
    }
    @Test void testUpdate() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L); dto.setYear(2026); dto.setMonth(6); dto.setAmount(800000.0); dto.setTaxRate(10.0); dto.setStatus("送付済");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        InvoiceDTO r = service.update(1L, dto);
        assertNotNull(r); assertEquals("送付済", r.getStatus());
    }
    @Test void testDelete() {
        when(invoiceRepository.existsById(1L)).thenReturn(true);
        service.delete(1L);
        verify(invoiceRepository).deleteById(1L);
    }
    @Test void testDelete_NotFound() {
        when(invoiceRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.delete(99L));
    }
    @Test void testExportCsv() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(detailRepository.findByInvoiceId(1L)).thenReturn(Collections.emptyList());
        when(bankAccountRepository.findByCustomerId(1L)).thenReturn(Collections.emptyList());
        String csv = service.exportInvoiceCsv(1L);
        assertNotNull(csv); assertFalse(csv.isEmpty());
    }
    @Test void testUploadDocument() throws Exception {
        when(invoiceRepository.existsById(1L)).thenReturn(true);
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        when(orderDocumentRepository.save(any(OrderDocument.class))).thenReturn(new OrderDocument());
        assertNotNull(service.uploadDocument(1L, file));
    }
    @Test void testGetDocumentFileName() {
        OrderDocument doc = new OrderDocument();
        doc.setId(1L); doc.setFileName("test.pdf");
        when(orderDocumentRepository.findById(1L)).thenReturn(Optional.of(doc));
        assertEquals("test.pdf", service.getDocumentFileName(1L));
    }

    @Test void testGetDocumentFile_docNotFound() {
        when(orderDocumentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getDocumentFile(99L));
    }

    @Test void testGetDocumentFile_fileNotExists() {
        OrderDocument doc = new OrderDocument();
        doc.setId(1L); doc.setFileName("ghost.pdf");
        doc.setFilePath("uploads/nonexistent_99999.pdf");
        when(orderDocumentRepository.findById(1L)).thenReturn(Optional.of(doc));
        assertThrows(RuntimeException.class, () -> service.getDocumentFile(1L));
    }

    // saveDetails 間接テスト (create経由)
    @Test void testCreate_withDetails_saveDetailsBranch() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L); dto.setYear(2026); dto.setMonth(6); dto.setAmount(500000.0); dto.setTaxRate(10.0);
        // Add 3 details → saveDetails loop executes 3 times
        List<InvoiceDetailDTO> details = new ArrayList<>();
        InvoiceDetailDTO d1 = new InvoiceDetailDTO(); d1.setEmployeeName("山田"); d1.setDescription("設計");
        d1.setQuantity(2.0); d1.setUnitPrice(250000.0); d1.setIsOvertime(false);
        InvoiceDetailDTO d2 = new InvoiceDetailDTO(); d2.setEmployeeName("鈴木"); d2.setDescription("開発");
        d2.setQuantity(3.0); d2.setUnitPrice(200000.0); d2.setIsOvertime(true);
        InvoiceDetailDTO d3 = new InvoiceDetailDTO(); d3.setEmployeeName("田中"); d3.setDescription("テスト");
        d3.setQuantity(1.0); d3.setUnitPrice(100000.0); d3.setIsOvertime(false);
        details.add(d1); details.add(d2); details.add(d3);
        dto.setDetails(details);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(detailRepository.save(any(InvoiceDetail.class))).thenReturn(new InvoiceDetail());
        InvoiceDTO r = service.create(dto);
        assertNotNull(r);
        verify(detailRepository, times(3)).save(any(InvoiceDetail.class));
    }

    // saveDetails null branch (early return)
    @Test void testCreate_withNullDetails_earlyReturn() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L); dto.setYear(2026); dto.setMonth(7); dto.setAmount(300000.0); dto.setTaxRate(10.0);
        dto.setDetails(null);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        InvoiceDTO r = service.create(dto);
        assertNotNull(r);
        verify(detailRepository, never()).save(any(InvoiceDetail.class));
    }

    // saveDetails with null quantity/unitPrice
    @Test void testCreate_withNullQuantity_saveDetailsNullBranch() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L); dto.setYear(2026); dto.setMonth(8); dto.setAmount(200000.0); dto.setTaxRate(10.0);
        List<InvoiceDetailDTO> details = new ArrayList<>();
        InvoiceDetailDTO d1 = new InvoiceDetailDTO(); d1.setEmployeeName("佐藤"); d1.setDescription("nullテスト");
        d1.setQuantity(null); d1.setUnitPrice(null); d1.setIsOvertime(null);
        details.add(d1);
        dto.setDetails(details);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(detailRepository.save(any(InvoiceDetail.class))).thenReturn(new InvoiceDetail());
        InvoiceDTO r = service.create(dto);
        assertNotNull(r);
        verify(detailRepository).save(any(InvoiceDetail.class));
    }
}
