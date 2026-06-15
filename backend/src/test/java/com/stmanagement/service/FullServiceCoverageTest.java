package com.stmanagement.service;

import com.stmanagement.dto.*;
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
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FullServiceCoverageTest {

    // === Invoice Service ===
    @Mock private InvoiceRepository invRepo; @Mock private InvoiceDetailRepository invDetRepo;
    @Mock private OrderDocumentRepository docRepo; @Mock private CustomerRepository custRepo;
    @Mock private EmployeeRepository empRepo; @Mock private BankAccountRepository baRepo;
    @InjectMocks private InvoiceService invService;

    // === Employee Service ===
    @Mock private EmployeeRepository eRepo; @Mock private EmployeeAttachmentRepository eAttRepo;
    @Mock private BankAccountRepository eBaRepo; @Mock private EntityManager em;
    @InjectMocks private EmployeeService empService;

    // === Supplier Order ===
    @Mock private SupplierOrderRepository soRepo; @Mock private SupplierOrderDetailRepository soDetRepo;
    @InjectMocks private SupplierOrderService soService;

    // === Bank Account ===
    @Mock private BankAccountRepository bRepo; @Mock private CustomerRepository bCustRepo;
    @Mock private EmployeeRepository bEmpRepo; @Mock private EntityManager bEm;
    @Mock private Query bQuery;
    @InjectMocks private BankAccountService baService;

    private Invoice inv; private Customer cust; private Employee emp;

    @BeforeEach void setUp() {
        cust = new Customer(); cust.setId(1L); cust.setCompanyName("テスト"); cust.setAddress("東京"); cust.setPhone("03-1111");
        inv = new Invoice(); inv.setId(1L); inv.setInvoiceNumber("INV-2026-0501"); inv.setCustomerId(1L);
        inv.setYear(2026); inv.setMonth(5); inv.setAmount(1000000.0); inv.setTaxRate(10.0);
        inv.setTaxAmount(100000.0); inv.setTotalWithTax(1100000.0); inv.setStatus("下書き");
        inv.setInvoiceDate(LocalDate.of(2026,5,1)); inv.setDueDate(LocalDate.of(2026,6,30));
        emp = new Employee(); emp.setId(1L); emp.setEmployeeCode("EMP0001"); emp.setName("山田");
        emp.setDepartment("営業部"); emp.setEmail("t@t.com"); emp.setStatus("在職"); emp.setJoinDate(LocalDate.of(2020,4,1));
    }

    // ---- INVOICE ----
    @Test void inv_uploadDoc() throws Exception {
        when(invRepo.existsById(1L)).thenReturn(true);
        OrderDocument doc = new OrderDocument(); doc.setId(1L); doc.setFileName("f.pdf"); doc.setFilePath("up/f.pdf"); doc.setFileSize(4L);
        when(docRepo.save(any())).thenReturn(doc);
        OrderDocumentDTO r = invService.uploadDocument(1L, new MockMultipartFile("f","f.pdf","application/pdf","d".getBytes()));
        assertEquals("f.pdf", r.getFileName());
    }
    @Test void inv_deleteDoc() {
        OrderDocument doc = new OrderDocument(); doc.setId(1L); doc.setFileName("d.pdf"); doc.setFilePath("up/d.pdf");
        when(docRepo.findById(1L)).thenReturn(Optional.of(doc));
        doNothing().when(docRepo).deleteById(1L);
        invService.deleteDocument(1L); verify(docRepo).deleteById(1L);
    }
    @Test void inv_docFileName() {
        OrderDocument doc = new OrderDocument(); doc.setId(1L); doc.setFileName("my.pdf");
        when(docRepo.findById(1L)).thenReturn(Optional.of(doc));
        assertEquals("my.pdf", invService.getDocumentFileName(1L));
    }
    @Test void inv_exportCsv_withBank() {
        when(invRepo.findById(1L)).thenReturn(Optional.of(inv)); when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        when(invDetRepo.findByInvoiceId(1L)).thenReturn(Collections.emptyList());
        BankAccount ba = new BankAccount(); ba.setBankName("銀行"); ba.setBranchCode("038"); ba.setAccountType("普通");
        ba.setAccountNumber("123"); ba.setAccountHolder("名義"); ba.setIsDefault(true);
        when(baRepo.findByCustomerId(1L)).thenReturn(Collections.singletonList(ba));
        assertTrue(invService.exportInvoiceCsv(1L).contains("銀行"));
    }
    @Test void inv_findById_full() {
        when(invRepo.findById(1L)).thenReturn(Optional.of(inv)); when(custRepo.findById(1L)).thenReturn(Optional.of(cust));
        InvoiceDetail d = new InvoiceDetail(); d.setId(1L); d.setEmployeeName("田中"); d.setDescription("設計"); d.setQuantity(1.0); d.setUnitPrice(500000.0); d.setAmount(500000.0);
        when(invDetRepo.findByInvoiceId(1L)).thenReturn(Collections.singletonList(d));
        OrderDocument doc = new OrderDocument(); doc.setId(1L); doc.setFileName("添付.pdf"); doc.setFilePath("u/a.pdf"); doc.setFileSize(100L);
        when(docRepo.findByInvoiceId(1L)).thenReturn(Collections.singletonList(doc));
        InvoiceDTO r = invService.findById(1L);
        assertEquals(1, r.getDetails().size()); assertEquals(1, r.getDocuments().size());
    }

    // ---- EMPLOYEE ----
    @Test void emp_uploadFiles() throws Exception {
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        MockMultipartFile f = new MockMultipartFile("f","a.pdf","application/pdf","abc".getBytes());
        EmployeeAttachment a = new EmployeeAttachment(); a.setFileName("a.pdf"); a.setFileSize(3L);
        when(eAttRepo.save(any())).thenReturn(a);
        assertEquals(1, empService.uploadFiles(1L, new MultipartFile[]{f}).size());
    }
    @Test void emp_batchImport() throws Exception {
        String csv = "EMP0501,花子,総務部,h@t.com\nEMP0502,健太,経理部,k@t.com";
        MockMultipartFile f = new MockMultipartFile("f","i.csv","text/csv",csv.getBytes("UTF-8"));
        when(eRepo.findAll()).thenReturn(Collections.emptyList()); when(eRepo.save(any())).thenReturn(emp);
        assertEquals(2, empService.batchImport(f));
    }
    @Test void emp_exportCsv() {
        when(eRepo.findAll()).thenReturn(Collections.singletonList(emp));
        assertTrue(empService.exportCsv().contains("EMP0001"));
    }
    @Test void emp_deleteAttachment() {
        EmployeeAttachment a = new EmployeeAttachment(); a.setId(1L); a.setFileName("d.pdf"); a.setFilePath("uploads/d.pdf");
        when(eAttRepo.findById(1L)).thenReturn(Optional.of(a)); doNothing().when(eAttRepo).deleteById(1L);
        empService.deleteAttachment(1L); verify(eAttRepo).deleteById(1L);
    }
    @Test void emp_fileName() {
        EmployeeAttachment a = new EmployeeAttachment(); a.setId(1L); a.setFileName("doc.pdf");
        when(eAttRepo.findById(1L)).thenReturn(Optional.of(a));
        assertEquals("doc.pdf", empService.getAttachmentFileName(1L));
    }

    // ---- SUPPLIER ORDER ----
    @Test void so_create_withAllFields() {
        SupplierOrder so = SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001").supplierName("T").orderDate(LocalDate.now()).amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0).status("下書き").build();
        when(soRepo.save(any())).thenReturn(so); when(soDetRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        Map<String,Object> d = new HashMap<>(); d.put("supplierName","T"); d.put("orderDate","2026-06-01");
        d.put("amount",1000000.0); d.put("taxRate",10.0); d.put("issuerName","山田"); d.put("issuerDept","営業部");
        d.put("supplierContact","佐々木"); d.put("supplierDept","開発部"); d.put("supplierTel","03-1111"); d.put("supplierAddr","東京");
        assertNotNull(soService.create(d));
    }
    @Test void so_pdfExport() throws Exception {
        SupplierOrder so = SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001").supplierName("T").orderDate(LocalDate.of(2026,5,10)).deliveryDate(LocalDate.of(2026,7,1)).subject("T").amount(5000000.0).taxRate(10.0).taxAmount(500000.0).totalWithTax(5500000.0).status("発注済").issuerName("山田").issuerDept("営業部").issuerTel("090-1111").supplierContact("佐々木").supplierDept("開発部").supplierTel("03-1234").supplierAddr("東京都港区").remark("備考").build();
        when(soRepo.findById(1L)).thenReturn(Optional.of(so));
        SupplierOrderDetail d = SupplierOrderDetail.builder().id(1L).orderId(1L).employeeName("山田").itemName("設計").quantity(1.0).unitPrice(3000000.0).amount(3000000.0).build();
        when(soDetRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(d));
        byte[] pdf = soService.exportPdf(1L);
        assertTrue(pdf.length > 3000);
    }

    // ---- BANK ACCOUNT ----
    @Test void ba_create_em() {
        BankAccountDTO d = baDto("EM銀行");
        when(bEm.createNativeQuery(contains("nextval"))).thenReturn(bQuery);
        when(bQuery.getSingleResult()).thenReturn(BigInteger.valueOf(600L));
        when(bEm.merge(any())).thenAnswer(i -> i.getArgument(0));
        when(bRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bCustRepo.findById(1L)).thenReturn(Optional.empty());
        assertEquals("BK000600", baService.create(d).getTorihikiNo());
    }
    @Test void ba_create_torihikiBranch() {
        BankAccountDTO d = baDto("枝番"); d.setTorihikiNo("BK000001");
        BankAccount b = new BankAccount(); b.setTorihikiNo("BK000001"); b.setBranchNo("003");
        when(bRepo.findByTorihikiNo("BK000001")).thenReturn(Collections.singletonList(b));
        when(bEm.merge(any())).thenAnswer(i -> i.getArgument(0));
        when(bRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bCustRepo.findById(1L)).thenReturn(Optional.empty());
        assertEquals("004", baService.create(d).getBranchNo());
    }
    @Test void ba_bindCustomer() {
        BankAccount b = new BankAccount(); b.setId(1L); b.setTorihikiNo(null);
        when(bRepo.findById(1L)).thenReturn(Optional.of(b));
        Customer c = new Customer(); c.setId(2L); c.setTorihikiNo("BK000099");
        when(bCustRepo.findById(2L)).thenReturn(Optional.of(c));
        when(bRepo.save(any())).thenReturn(b);
        baService.bindToCustomer(1L, 2L); verify(bRepo).save(any());
    }

    private BankAccountDTO baDto(String name) {
        BankAccountDTO d = new BankAccountDTO();
        d.setBankName(name); d.setAccountType("普通"); d.setAccountNumber("123"); d.setAccountHolder("名義");
        d.setCategory("CUSTOMER"); d.setCustomerId(1L); return d;
    }
}
