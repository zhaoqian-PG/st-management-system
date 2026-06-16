package com.stmanagement.integration;

import com.stmanagement.dto.*;
import com.stmanagement.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class FullCoverageIT {
    @Autowired private BankAccountService ba;
    @Autowired private EmployeeService emp;
    @Autowired private InvoiceService inv;
    @Autowired private PurchaseOrderService po;
    @Autowired private SupplierOrderService so;
    @Autowired private AttendanceService att;
    @TempDir Path dir;

    @Test void ba01_createEM() {
        BankAccountDTO d = baDto("EM銀行"); BankAccountDTO c = ba.create(d);
        assertTrue(c.getTorihikiNo().startsWith("BK")); ba.delete(c.getId());
    }
    @Test void ba02_createTorihiki() {
        BankAccountDTO d = baDto("枝番"); d.setTorihikiNo("BK000001");
        BankAccountDTO c = ba.create(d);
        assertNotNull(c.getBranchNo()); ba.delete(c.getId());
    }
    @Test void emp01_upload() throws Exception {
        EmployeeDTO e = new EmployeeDTO(); e.setName("IT太郎"); e.setDepartment("開発部");
        e.setEmail("it@t.com"); e.setJoinDate(LocalDate.now()); e.setStatus("在職");
        EmployeeDTO c = emp.create(e);
        Path f = dir.resolve("it.pdf"); Files.write(f, "hello".getBytes());
        emp.uploadFiles(c.getId(), new org.springframework.web.multipart.MultipartFile[]{
            new MockMultipartFile("f","it.pdf","application/pdf",Files.readAllBytes(f))});
        emp.delete(c.getId());
    }
    @Test void emp02_batch() throws Exception {
        String csv = "EMP5001,IT花子,総務部,ith@t.com";
        assertEquals(1, emp.batchImport(new MockMultipartFile("f","i.csv","text/csv",csv.getBytes("UTF-8"))));
    }
    @Test void inv01_upload() throws Exception {
        InvoiceDTO d = new InvoiceDTO(); d.setCustomerId(1L); d.setYear(2026); d.setMonth(11);
        d.setAmount(100000.0); d.setTaxRate(10.0);
        InvoiceDTO c = inv.create(d);
        OrderDocumentDTO doc = inv.uploadDocument(c.getId(),
            new MockMultipartFile("f","請求IT.pdf","application/pdf","data".getBytes()));
        assertNotNull(doc); inv.deleteDocument(doc.getId()); inv.delete(c.getId());
    }
    @Test void po01_attach() throws Exception {
        PurchaseOrderDTO d = PurchaseOrderDTO.builder().customerId(1L)
            .orderDate(LocalDate.now()).deliveryDate(LocalDate.now().plusMonths(1))
            .subject("IT").amount(100000.0).taxRate(10.0).build();
        PurchaseOrderDTO c = po.create(d);
        po.uploadAttachment(c.getId(),
            new MockMultipartFile("f","poIT.pdf","application/pdf","xyz".getBytes()));
        PurchaseOrderDTO f = po.findById(c.getId());
        if(!f.getAttachments().isEmpty()) po.deleteAttachment(f.getAttachments().get(0).getId());
        po.delete(c.getId());
    }
    @Test void so01_pdf() throws Exception {
        Map<String,Object> d = new HashMap<>(); d.put("supplierName","IT発注先");
        d.put("orderDate",LocalDate.now().toString()); d.put("amount",1000000.0); d.put("taxRate",10.0);
        Map<String,Object> c = so.create(d);
        assertTrue(so.exportPdf((Long)c.get("id")).length > 1000);
        so.delete((Long)c.get("id"));
    }
    @Test void att01_gen() {
        assertTrue(att.generateMonth(2026,10,1L) >= 0);
        assertTrue(att.generateMonthForAll(2026,11) >= 0);
    }
    private BankAccountDTO baDto(String n){ BankAccountDTO d=new BankAccountDTO();
        d.setBankName(n); d.setAccountType("普通"); d.setAccountNumber("1112223");
        d.setAccountHolder("IT"); d.setCategory("CUSTOMER"); d.setCustomerId(1L); return d; }
}
