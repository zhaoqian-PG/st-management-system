package com.stmanagement.integration;

import com.stmanagement.dto.*;
import com.stmanagement.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AllInOneTest {

    @Autowired private BankAccountService ba;
    @Autowired private EmployeeService emp;
    @Autowired private InvoiceService inv;
    @Autowired private PurchaseOrderService po;
    @Autowired private SupplierOrderService so;
    @Autowired private AttendanceService att;
    @Autowired private CustomerService cust;
    @TempDir Path dir;

    // ======== BankAccount: EntityManager native query ========
    @Test @Order(1) void ba_em_create() {
        BankAccountDTO d = baDto("EM統合銀行");
        BankAccountDTO c = ba.create(d);
        assertTrue(c.getTorihikiNo().startsWith("BK"));
        assertNotNull(c.getTorihikiNo());
        ba.delete(c.getId());
    }

    @Test @Order(2) void ba_em_createWithTorihiki() {
        BankAccountDTO d = baDto("枝番銀行"); d.setTorihikiNo("BK000001");
        BankAccountDTO c = ba.create(d);
        assertNotNull(c.getBranchNo());
        assertTrue(Integer.parseInt(c.getBranchNo()) >= 1);
        ba.delete(c.getId());
    }

    @Test @Order(3) void ba_findMethods() {
        assertNotNull(ba.findByCustomerId(1L));
        assertNotNull(ba.findByEmployeeId(1L));
        assertNotNull(ba.nextBranchNo("BK000001"));
        assertNotNull(ba.getExistingTorihikiNos("CUSTOMER"));
        assertNotNull(ba.getExistingTorihikiNos("EMPLOYEE"));
    }

    @Test @Order(4) void ba_fullCrud() {
        BankAccountDTO d = baDto("CRUD銀行");
        BankAccountDTO c = ba.create(d);
        Long id = c.getId();
        assertNotNull(ba.findById(id));

        java.util.List<BankAccountDTO> list = ba.findAll("CUSTOMER", null, 0, 10).getContent();
        assertNotNull(list); assertFalse(list.isEmpty());

        c.setBankName("更新銀行");
        ba.update(id, c);
        assertEquals("更新銀行", ba.findById(id).getBankName());

        ba.delete(id);
    }

    // ======== Employee: File I/O ========
    @Test @Order(5) void emp_uploadFiles() throws Exception {
        EmployeeDTO e = new EmployeeDTO(); e.setName("統合太郎"); e.setDepartment("開発部");
        e.setEmail("allinone@t.com"); e.setJoinDate(LocalDate.now()); e.setStatus("在職");
        EmployeeDTO c = emp.create(e);

        Path f = dir.resolve("aio.pdf"); Files.write(f, "hello world".getBytes());
        emp.uploadFiles(c.getId(), new org.springframework.web.multipart.MultipartFile[]{
            new MockMultipartFile("files","aio.pdf","application/pdf",Files.readAllBytes(f))});
        emp.delete(c.getId());
    }

    @Test @Order(6) void emp_batchImport() throws Exception {
        String csv = "EMP6001,一郎,法務部,ichiro@t.com\nEMP6002,二郎,経理部,jiro@t.com";
        int n = emp.batchImport(new MockMultipartFile("f","i.csv","text/csv",csv.getBytes("UTF-8")));
        assertTrue(n > 0);
    }

    @Test @Order(7) void emp_exportCsv() {
        assertNotNull(emp.exportCsv());
    }

    @Test @Order(8) void emp_fullCrud() {
        EmployeeDTO e = new EmployeeDTO(); e.setName("CRUD社員"); e.setDepartment("営業部");
        e.setEmail("crud@t.com"); e.setJoinDate(LocalDate.now()); e.setStatus("在職");
        EmployeeDTO c = emp.create(e);
        assertNotNull(emp.findById(c.getId()));

        c.setDepartment("経理部");
        emp.update(c.getId(), c);
        assertEquals("経理部", emp.findById(c.getId()).getDepartment());

        emp.delete(c.getId());
    }

    // ======== Invoice: File I/O + CSV ========
    @Test @Order(9) void inv_uploadDoc() throws Exception {
        InvoiceDTO d = new InvoiceDTO(); d.setCustomerId(1L); d.setYear(2026); d.setMonth(12);
        d.setAmount(888000.0); d.setTaxRate(10.0);
        InvoiceDTO c = inv.create(d);
        OrderDocumentDTO doc = inv.uploadDocument(c.getId(),
            new MockMultipartFile("f","請求書統合.pdf","application/pdf","data".getBytes()));
        assertNotNull(doc); assertEquals("請求書統合.pdf", doc.getFileName());
        inv.deleteDocument(doc.getId()); inv.delete(c.getId());
    }

    @Test @Order(10) void inv_exportCsv() {
        InvoiceDTO d = new InvoiceDTO(); d.setCustomerId(1L); d.setYear(2026); d.setMonth(11);
        d.setAmount(999000.0); d.setTaxRate(10.0); d.setDueDate(LocalDate.of(2026,12,31));
        InvoiceDTO c = inv.create(d);
        String csv = inv.exportInvoiceCsv(c.getId());
        assertNotNull(csv); assertFalse(csv.isEmpty());
        inv.delete(c.getId());
    }

    @Test @Order(11) void inv_fullCrud() {
        InvoiceDTO d = new InvoiceDTO(); d.setCustomerId(1L); d.setYear(2026); d.setMonth(10);
        d.setAmount(500000.0); d.setTaxRate(10.0);
        InvoiceDTO c = inv.create(d);
        assertNotNull(c);
        assertNotNull(inv.findById(c.getId()));
        inv.delete(c.getId());
    }

    // ======== PurchaseOrder: Attachment + CSV ========
    @Test @Order(12) void po_createAndDelete() throws Exception {
        PurchaseOrderDTO d = PurchaseOrderDTO.builder().customerId(1L)
            .orderDate(LocalDate.now()).deliveryDate(LocalDate.now().plusMonths(1))
            .subject("統合PO").amount(555000.0).taxRate(10.0).build();
        PurchaseOrderDTO c = po.create(d);
        assertNotNull(c);
        po.delete(c.getId());
    }

    @Test @Order(13) void po_attachAndFind() throws Exception {
        PurchaseOrderDTO d = PurchaseOrderDTO.builder().customerId(1L)
            .orderDate(LocalDate.now()).deliveryDate(LocalDate.now().plusMonths(1))
            .subject("統合PO2").amount(555000.0).taxRate(10.0).build();
        PurchaseOrderDTO c = po.create(d);
        po.uploadAttachment(c.getId(),
            new MockMultipartFile("f","po統合.pdf","application/pdf","xyz".getBytes()));
        PurchaseOrderDTO found = po.findById(c.getId());
        assertFalse(found.getAttachments().isEmpty());
        po.deleteAttachment(found.getAttachments().get(0).getId());
        po.delete(c.getId());
    }

    // ======== SupplierOrder: PDF ========
    @Test @Order(14) void so_createAndPdf() throws Exception {
        Map<String,Object> d = new HashMap<>();
        d.put("supplierName","統合発注先"); d.put("orderDate",LocalDate.now().toString());
        d.put("deliveryDate",LocalDate.now().plusMonths(1).toString());
        d.put("amount",7770000.0); d.put("taxRate",10.0); d.put("subject","統合テスト");
        d.put("issuerName","統合発注者"); d.put("issuerDept","統合発注部"); d.put("issuerTel","090-0000");
        d.put("supplierContact","統合受注者"); d.put("supplierDept","統合受注部");
        d.put("supplierTel","03-9999"); d.put("supplierAddr","東京都千代田区");
        Map<String,Object> c = so.create(d);
        byte[] pdf = so.exportPdf((Long)c.get("id"));
        assertTrue(pdf.length > 3000);
        so.delete((Long)c.get("id"));
    }

    @Test @Order(15) void so_fullCrud() {
        Map<String,Object> d = new HashMap<>();
        d.put("supplierName","CRUD発注先"); d.put("orderDate",LocalDate.now().toString());
        d.put("amount",2000000.0); d.put("taxRate",10.0);
        Map<String,Object> c = so.create(d);

        Map<String,Object> found = so.findById((Long)c.get("id"));
        assertNotNull(found);

        so.delete((Long)c.get("id"));
    }

    // ======== Attendance: generate + export ========
    @Test @Order(16) void att_generateMonths() {
        assertTrue(att.generateMonth(2026, 10, 1L) >= 0);
        assertTrue(att.generateMonthForAll(2026, 11) >= 0);
    }

    @Test @Order(17) void att_exportAndSummary() {
        assertNotNull(att.exportCsv(2026, 10, 1L));
        assertNotNull(att.exportCsvAll(2026, 10));
        assertNotNull(att.getMonthlySummary(2026, 10, 1L));
        assertNotNull(att.getAllEmployeeMonthlySummary(2026, 10));
    }

    @Test @Order(18) void att_fullCrud() {
        AttendanceDTO d = new AttendanceDTO();
        d.setEmployeeId(1L); d.setWorkDate(LocalDate.now()); d.setWorkHours(8.0);
        d.setWorkType("NORMAL"); d.setStatus("出勤");
        AttendanceDTO c = att.create(d);
        assertNotNull(att.findById(c.getId()));

        c.setWorkHours(7.0); c.setOvertimeHours(1.0);
        att.update(c.getId(), c);
        assertEquals(7.0, att.findById(c.getId()).getWorkHours());

        att.delete(c.getId());
    }

    // ======== Customer ========
    @Test @Order(19) void cust_fullCrud() {
        CustomerDTO d = new CustomerDTO(); d.setCompanyName("統合カスタマー");
        d.setPresidentName("社長"); d.setEmail("cust@t.com"); d.setPhone("03-1111");
        CustomerDTO c = cust.create(d);

        c.setCompanyName("更新カスタマー");
        cust.update(c.getId(), c);
        assertEquals("更新カスタマー", cust.findById(c.getId()).getCompanyName());

        cust.delete(c.getId());
    }

    private BankAccountDTO baDto(String n) {
        BankAccountDTO d = new BankAccountDTO();
        d.setBankName(n); d.setAccountType("普通"); d.setAccountNumber("8889991");
        d.setAccountHolder("統合名義"); d.setCategory("CUSTOMER"); d.setCustomerId(1L);
        return d;
    }
}
