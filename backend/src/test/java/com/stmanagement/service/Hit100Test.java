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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Hit100Test {

    // Invoice
    @Mock private InvoiceRepository iRepo; @Mock private InvoiceDetailRepository iDetRepo;
    @Mock private OrderDocumentRepository dRepo; @Mock private CustomerRepository cRepo;
    @Mock private EmployeeRepository eRepo; @Mock private BankAccountRepository bRepo;
    @InjectMocks private InvoiceService invSvc;
    // PO
    @Mock private PurchaseOrderRepository poRepo; @Mock private PurchaseOrderDetailRepository poDetRepo;
    @Mock private PurchaseOrderAttachmentRepository poAttRepo;
    @InjectMocks private PurchaseOrderService poSvc;
    // Employee
    @Mock private EmployeeRepository empRepo; @Mock private EmployeeAttachmentRepository empAttRepo;
    @Mock private BankAccountRepository empBaRepo; @Mock private EntityManager em;
    @InjectMocks private EmployeeService empSvc;
    // BA
    @Mock private BankAccountRepository baRepo; @Mock private CustomerRepository baCustRepo;
    @Mock private EmployeeRepository baEmpRepo; @Mock private EntityManager baEm;
    @Mock private Query baQuery;
    @InjectMocks private BankAccountService baSvc;
    // SO
    @Mock private SupplierOrderRepository soRepo; @Mock private SupplierOrderDetailRepository soDetRepo;
    @InjectMocks private SupplierOrderService soSvc;

    private Invoice invData; private Customer custData; private Employee empData; private PurchaseOrder poData;

    @BeforeEach void setUp() {
        custData = new Customer(); custData.setId(1L); custData.setCompanyName("Test"); custData.setAddress("Tokyo"); custData.setPhone("03-1111");
        invData = new Invoice(); invData.setId(1L); invData.setInvoiceNumber("INV-2026-0501"); invData.setCustomerId(1L); invData.setYear(2026); invData.setMonth(5); invData.setAmount(1000000.0); invData.setTaxRate(10.0); invData.setTaxAmount(100000.0); invData.setTotalWithTax(1100000.0); invData.setStatus("下書き"); invData.setInvoiceDate(LocalDate.of(2026,5,1)); invData.setDueDate(LocalDate.of(2026,6,30));
        empData = new Employee(); empData.setId(1L); empData.setEmployeeCode("EMP0001"); empData.setName("山田"); empData.setDepartment("営業部"); empData.setEmail("t@t.com"); empData.setStatus("在職"); empData.setJoinDate(LocalDate.of(2020,4,1));
        poData = PurchaseOrder.builder().id(1L).orderNumber("PO-2026-0001").customerId(1L).orderDate(LocalDate.of(2026,5,1)).amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0).status("下書き").build();
    }

    // === INVOICE (all methods) ===
    @Test void inv01_findAll() { when(iRepo.findAll(any(Specification.class),any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(invData))); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); assertEquals(1,invSvc.findAll(null,null,null,0,10).getTotalElements()); }
    @Test void inv02_findById() { when(iRepo.findById(1L)).thenReturn(Optional.of(invData)); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); when(iDetRepo.findByInvoiceId(1L)).thenReturn(Collections.emptyList()); when(dRepo.findByInvoiceId(1L)).thenReturn(Collections.emptyList()); assertNotNull(invSvc.findById(1L)); }
    @Test void inv03_create() { InvoiceDTO d=new InvoiceDTO(); d.setCustomerId(1L); d.setYear(2026); d.setMonth(6); d.setAmount(500000.0); d.setTaxRate(10.0); when(iRepo.save(any())).thenReturn(invData); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); assertNotNull(invSvc.create(d)); }
    @Test void inv04_update() { InvoiceDTO d=new InvoiceDTO(); d.setCustomerId(1L); d.setYear(2026); d.setMonth(6); d.setAmount(800000.0); d.setTaxRate(10.0); d.setStatus("送付済"); when(iRepo.findById(1L)).thenReturn(Optional.of(invData)); when(iRepo.save(any())).thenReturn(invData); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); assertEquals("送付済",invSvc.update(1L,d).getStatus()); }
    @Test void inv05_delete() { when(iRepo.existsById(1L)).thenReturn(true); doNothing().when(iRepo).deleteById(1L); invSvc.delete(1L); verify(iRepo).deleteById(1L); }
    @Test void inv06_uploadDoc() throws Exception { when(iRepo.existsById(1L)).thenReturn(true); OrderDocument doc=new OrderDocument(); doc.setId(1L); doc.setFileName("f.pdf"); doc.setFilePath("up/f.pdf"); doc.setFileSize(4L); when(dRepo.save(any())).thenReturn(doc); assertEquals("f.pdf",invSvc.uploadDocument(1L,new MockMultipartFile("f","f.pdf","application/pdf","d".getBytes())).getFileName()); }
    @Test void inv07_deleteDoc() { OrderDocument d=new OrderDocument(); d.setId(1L); d.setFileName("x.pdf"); d.setFilePath("up/x.pdf"); when(dRepo.findById(1L)).thenReturn(Optional.of(d)); doNothing().when(dRepo).deleteById(1L); invSvc.deleteDocument(1L); verify(dRepo).deleteById(1L); }
    @Test void inv08_docFileName() { OrderDocument d=new OrderDocument(); d.setId(1L); d.setFileName("my.pdf"); when(dRepo.findById(1L)).thenReturn(Optional.of(d)); assertEquals("my.pdf",invSvc.getDocumentFileName(1L)); }
    @Test void inv09_exportCsv() { when(iRepo.findById(1L)).thenReturn(Optional.of(invData)); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); when(iDetRepo.findByInvoiceId(1L)).thenReturn(Collections.emptyList()); BankAccount ba=new BankAccount(); ba.setBankName("銀行"); ba.setBranchCode("038"); ba.setAccountType("普通"); ba.setAccountNumber("123"); ba.setAccountHolder("名義"); ba.setIsDefault(true); when(bRepo.findByCustomerId(1L)).thenReturn(Collections.singletonList(ba)); assertTrue(invSvc.exportInvoiceCsv(1L).contains("銀行")); }
    @Test void inv10_findAllFilters() { when(iRepo.findAll(any(Specification.class),any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(invData))); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); assertEquals(1,invSvc.findAll(2026,5,1L,0,10).getTotalElements()); }

    // === PURCHASE ORDER ===
    @Test void po01_findAll() { when(poRepo.findAll(any(Specification.class),any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(poData))); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); assertEquals(1,poSvc.findAll(null,null,0,10).getTotalElements()); }
    @Test void po02_findById() { when(poRepo.findById(1L)).thenReturn(Optional.of(poData)); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); when(poDetRepo.findByOrderId(1L)).thenReturn(Collections.emptyList()); when(poAttRepo.findByOrderId(1L)).thenReturn(Collections.emptyList()); assertNotNull(poSvc.findById(1L)); }
    @Test void po03_create() { PurchaseOrderDTO d=PurchaseOrderDTO.builder().customerId(1L).orderDate(LocalDate.now()).deliveryDate(LocalDate.now().plusMonths(1)).subject("新規").amount(500000.0).taxRate(10.0).build(); when(poRepo.save(any())).thenReturn(poData); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); assertNotNull(poSvc.create(d)); }
    @Test void po04_update() { PurchaseOrderDTO d=PurchaseOrderDTO.builder().customerId(1L).orderDate(LocalDate.now()).amount(800000.0).taxRate(10.0).status("発注済").build(); when(poRepo.findById(1L)).thenReturn(Optional.of(poData)); when(poRepo.save(any())).thenReturn(poData); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); assertEquals("発注済",poSvc.update(1L,d).getStatus()); }
    @Test void po05_delete() { doNothing().when(poDetRepo).deleteByOrderId(1L); doNothing().when(poAttRepo).deleteByOrderId(1L); doNothing().when(poRepo).deleteById(1L); poSvc.delete(1L); verify(poDetRepo).deleteByOrderId(1L); verify(poAttRepo).deleteByOrderId(1L); verify(poRepo).deleteById(1L); }
    @Test void po06_exportCsv() { when(poRepo.findById(1L)).thenReturn(Optional.of(poData)); when(cRepo.findById(1L)).thenReturn(Optional.of(custData)); PurchaseOrderDetail d=PurchaseOrderDetail.builder().id(1L).orderId(1L).employeeName("山田").itemName("サーバー").quantity(2.0).unitPrice(500000.0).amount(1000000.0).build(); when(poDetRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(d)); assertTrue(poSvc.exportOrderCsv(1L).contains("サーバー")); }

    // === EMPLOYEE ===
    @Test void emp01_findAll() { when(empRepo.findAll(any(Specification.class),any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(empData))); assertEquals(1,empSvc.findAll(null,null,false,0,10).getTotalElements()); }
    @Test void emp02_findById() { when(empRepo.findById(1L)).thenReturn(Optional.of(empData)); assertNotNull(empSvc.findById(1L)); }
    @Test void emp03_create() { EmployeeDTO d=new EmployeeDTO(); d.setName("新規"); d.setDepartment("技術部"); when(empRepo.save(any())).thenReturn(empData); assertNotNull(empSvc.create(d)); }
    @Test void emp04_update() { EmployeeDTO d=new EmployeeDTO(); d.setName("更新"); d.setDepartment("経理部"); when(empRepo.findById(1L)).thenReturn(Optional.of(empData)); when(empRepo.save(any())).thenReturn(empData); assertNotNull(empSvc.update(1L,d)); }
    @Test void emp05_delete() { when(empRepo.existsById(1L)).thenReturn(true); doNothing().when(empRepo).deleteById(1L); empSvc.delete(1L); verify(empRepo).deleteById(1L); }
    @Test void emp06_uploadFiles() throws Exception { when(empRepo.findById(1L)).thenReturn(Optional.of(empData)); MockMultipartFile f=new MockMultipartFile("f","a.pdf","application/pdf","abc".getBytes()); EmployeeAttachment a=new EmployeeAttachment(); a.setFileName("a.pdf"); a.setFileSize(3L); when(empAttRepo.save(any())).thenReturn(a); assertEquals(1,empSvc.uploadFiles(1L,new org.springframework.web.multipart.MultipartFile[]{f}).size()); }
    @Test void emp07_batchImport() throws Exception { String csv="EMP3001,花子100,総務部,h100@t.com\nEMP3002,健太100,経理部,k100@t.com"; MockMultipartFile f=new MockMultipartFile("f","i.csv","text/csv",csv.getBytes("UTF-8")); when(empRepo.findAll()).thenReturn(Collections.emptyList()); when(empRepo.save(any())).thenReturn(empData); assertEquals(2,empSvc.batchImport(f)); }
    @Test void emp08_exportCsv() { when(empRepo.findAll()).thenReturn(Collections.singletonList(empData)); assertTrue(empSvc.exportCsv().contains("EMP0001")); }
    @Test void emp09_deleteAttachment() { EmployeeAttachment a=new EmployeeAttachment(); a.setId(1L); a.setFileName("d.pdf"); a.setFilePath("uploads/d.pdf"); when(empAttRepo.findById(1L)).thenReturn(Optional.of(a)); doNothing().when(empAttRepo).deleteById(1L); empSvc.deleteAttachment(1L); verify(empAttRepo).deleteById(1L); }
    @Test void emp10_getAttachFileName() { EmployeeAttachment a=new EmployeeAttachment(); a.setId(1L); a.setFileName("doc.pdf"); when(empAttRepo.findById(1L)).thenReturn(Optional.of(a)); assertEquals("doc.pdf",empSvc.getAttachmentFileName(1L)); }

    // === BANK ACCOUNT (EntityManager) ===
    @Test void ba01_create_em() { BankAccountDTO d=baDto("EM100"); when(baEm.createNativeQuery(contains("nextval"))).thenReturn(baQuery); when(baQuery.getSingleResult()).thenReturn(BigInteger.valueOf(900L)); when(baEm.merge(any())).thenAnswer(i->i.getArgument(0)); when(baRepo.save(any())).thenAnswer(i->i.getArgument(0)); when(baCustRepo.findById(1L)).thenReturn(Optional.empty()); assertEquals("BK000900",baSvc.create(d).getTorihikiNo()); }
    @Test void ba02_create_torihiki() { BankAccountDTO d=baDto("枝番100"); d.setTorihikiNo("BK000001"); BankAccount b=new BankAccount(); b.setTorihikiNo("BK000001"); b.setBranchNo("003"); when(baRepo.findByTorihikiNo("BK000001")).thenReturn(Collections.singletonList(b)); when(baEm.merge(any())).thenAnswer(i->i.getArgument(0)); when(baRepo.save(any())).thenAnswer(i->i.getArgument(0)); when(baCustRepo.findById(1L)).thenReturn(Optional.empty()); assertEquals("004",baSvc.create(d).getBranchNo()); }
    @Test void ba03_findAll() { when(baRepo.findAll(any(Specification.class),any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(baAccount()))); assertEquals(1,baSvc.findAll(null,null,0,10).getTotalElements()); }
    @Test void ba04_findById() { when(baRepo.findById(1L)).thenReturn(Optional.of(baAccount())); assertNotNull(baSvc.findById(1L)); }
    @Test void ba05_findByCustomer() { when(baRepo.findByCustomerId(1L)).thenReturn(Collections.singletonList(baAccount())); assertEquals(1,baSvc.findByCustomerId(1L).size()); }
    @Test void ba06_bindCustomer() { BankAccount b=new BankAccount(); b.setId(1L); b.setTorihikiNo(null); when(baRepo.findById(1L)).thenReturn(Optional.of(b)); Customer c=new Customer(); c.setId(2L); c.setTorihikiNo("BK000099"); when(baCustRepo.findById(2L)).thenReturn(Optional.of(c)); when(baRepo.save(any())).thenReturn(b); baSvc.bindToCustomer(1L,2L); verify(baRepo).save(any()); }
    @Test void ba07_unbindCustomer() { BankAccount b=baAccount(); when(baRepo.findById(1L)).thenReturn(Optional.of(b)); when(baRepo.save(any())).thenReturn(b); baSvc.unbindFromCustomer(1L); verify(baRepo).save(any()); }

    // === SUPPLIER ORDER ===
    @Test void so01_findAll() { SupplierOrder soE=SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001").supplierName("T").orderDate(LocalDate.now()).amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0).status("下書き").build(); when(soRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(soE))); assertEquals(1,soSvc.findAll(0,10).getTotalElements()); }
    @Test void so02_findById() { SupplierOrder soE=SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001").supplierName("T").orderDate(LocalDate.now()).amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0).status("下書き").build(); when(soRepo.findById(1L)).thenReturn(Optional.of(soE)); when(soDetRepo.findByOrderId(1L)).thenReturn(Collections.emptyList()); assertNotNull(soSvc.findById(1L)); }
    @Test void so03_create() { SupplierOrder soE=SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001").supplierName("T").orderDate(LocalDate.now()).amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0).status("下書き").build(); when(soRepo.save(any())).thenReturn(soE); when(soDetRepo.findByOrderId(1L)).thenReturn(Collections.emptyList()); Map<String,Object> d=new HashMap<>(); d.put("supplierName","T"); d.put("orderDate","2026-06-01"); d.put("amount",1000000.0); d.put("taxRate",10.0); assertNotNull(soSvc.create(d)); }
    @Test void so04_update() { SupplierOrder soE=SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001").supplierName("T").orderDate(LocalDate.now()).amount(1000000.0).taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0).status("下書き").build(); when(soRepo.findById(1L)).thenReturn(Optional.of(soE)); when(soRepo.save(any())).thenReturn(soE); when(soDetRepo.findByOrderId(1L)).thenReturn(Collections.emptyList()); Map<String,Object> d=new HashMap<>(); d.put("supplierName","U"); d.put("orderDate","2026-07-01"); d.put("amount",2000000.0); d.put("taxRate",10.0); assertNotNull(soSvc.update(1L,d)); }
    @Test void so05_delete() { doNothing().when(soDetRepo).deleteByOrderId(1L); doNothing().when(soRepo).deleteById(1L); soSvc.delete(1L); verify(soDetRepo).deleteByOrderId(1L); verify(soRepo).deleteById(1L); }
    @Test void so06_exportPdf() throws Exception { SupplierOrder soE=SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001").supplierName("T").orderDate(LocalDate.of(2026,5,10)).deliveryDate(LocalDate.of(2026,7,1)).subject("T").amount(5000000.0).taxRate(10.0).taxAmount(500000.0).totalWithTax(5500000.0).status("発注済").issuerName("山田").issuerDept("営業部").issuerTel("090-1111").supplierContact("佐々木").supplierDept("開発部").supplierTel("03-1234").supplierAddr("東京都港区").remark("備考").build(); when(soRepo.findById(1L)).thenReturn(Optional.of(soE)); SupplierOrderDetail dd=SupplierOrderDetail.builder().id(1L).orderId(1L).employeeName("山田").itemName("設計").quantity(1.0).unitPrice(3000000.0).amount(3000000.0).build(); when(soDetRepo.findByOrderId(1L)).thenReturn(Collections.singletonList(dd)); byte[] pdf=soSvc.exportPdf(1L); assertTrue(pdf.length>3000); }

    private BankAccountDTO baDto(String name){ BankAccountDTO d=new BankAccountDTO(); d.setBankName(name); d.setAccountType("普通"); d.setAccountNumber("123"); d.setAccountHolder("名義"); d.setCategory("CUSTOMER"); d.setCustomerId(1L); return d; }
    private BankAccount baAccount(){ BankAccount b=new BankAccount(); b.setId(1L); b.setTorihikiNo("BK000001"); b.setBranchNo("001"); b.setCategory("CUSTOMER"); b.setCustomerId(1L); return b; }
}
