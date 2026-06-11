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
        customer.setAddress("東京都千代田区");
        invoice = new Invoice();
        invoice.setId(1L); invoice.setInvoiceNumber("INV-2026-0501"); invoice.setCustomerId(1L);
        invoice.setYear(2026); invoice.setMonth(5); invoice.setAmount(1500000.0);
        invoice.setTaxRate(10.0); invoice.setTaxAmount(150000.0); invoice.setTotalWithTax(1650000.0);
        invoice.setStatus("下書き"); invoice.setSubject("システム開発費用");
        invoice.setInvoiceDate(LocalDate.of(2026, 5, 1));
        invoice.setDueDate(LocalDate.of(2026, 6, 30));
    }

    @Test
    void testFindAll() {
        List<Invoice> invoices = Collections.singletonList(invoice);
        Page<Invoice> page = new PageImpl<>(invoices);
        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        Page<InvoiceDTO> result = service.findAll(null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("INV-2026-0501", result.getContent().get(0).getInvoiceNumber());
    }

    @Test
    void testFindById() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(detailRepository.findByInvoiceId(1L)).thenReturn(Collections.emptyList());
        when(orderDocumentRepository.findByInvoiceId(1L)).thenReturn(Collections.emptyList());

        InvoiceDTO result = service.findById(1L);

        assertNotNull(result);
        assertEquals("INV-2026-0501", result.getInvoiceNumber());
        assertNotNull(result.getDetails());
        assertNotNull(result.getDocuments());
    }

    @Test
    void testFindById_NotFound() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }

    @Test
    void testCreate() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L); dto.setYear(2026); dto.setMonth(6);
        dto.setAmount(500000.0); dto.setTaxRate(10.0); dto.setSubject("新規請求");

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        InvoiceDTO result = service.create(dto);

        assertNotNull(result);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void testUpdate() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L); dto.setYear(2026); dto.setMonth(6);
        dto.setAmount(800000.0); dto.setTaxRate(10.0);
        dto.setStatus("送付済"); dto.setSubject("更新請求");

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        InvoiceDTO result = service.update(1L, dto);

        assertNotNull(result);
        assertEquals("送付済", result.getStatus());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void testDelete() {
        when(invoiceRepository.existsById(1L)).thenReturn(true);
        doNothing().when(invoiceRepository).deleteById(1L);

        service.delete(1L);

        verify(invoiceRepository).deleteById(1L);
    }

    @Test
    void testDelete_NotFound() {
        when(invoiceRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.delete(99L));
    }

    @Test
    void testExportCsv() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(detailRepository.findByInvoiceId(1L)).thenReturn(Collections.emptyList());

        String csv = service.exportInvoiceCsv(1L);

        assertNotNull(csv);
        assertTrue(csv.contains("INV-2026-0501"));
        assertTrue(csv.contains("テスト株式会社"));
    }
}
