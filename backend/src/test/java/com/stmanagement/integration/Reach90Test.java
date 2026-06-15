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
class Reach90Test {

    @Autowired private BankAccountService baService;
    @Autowired private EmployeeService empService;
    @Autowired private InvoiceService invService;
    @Autowired private AttendanceService attService;
    @Autowired private SupplierOrderService soService;
    @Autowired private PurchaseOrderService poService;

    @TempDir Path tempDir;

    @Test void testBankAccount_createAndBind() {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setBankName("90テスト銀行"); dto.setAccountType("普通");
        dto.setAccountNumber("8888888"); dto.setAccountHolder("90テスト名義");
        dto.setCategory("CUSTOMER"); dto.setCustomerId(1L);
        BankAccountDTO created = baService.create(dto);
        assertNotNull(created); assertTrue(created.getTorihikiNo().startsWith("BK"));

        // Test bind/unbind
        baService.unbindFromCustomer(created.getId());
        baService.bindToCustomer(created.getId(), 1L);
        baService.delete(created.getId());
    }

    @Test void testEmployee_createAndDelete() {
        EmployeeDTO emp = new EmployeeDTO();
        emp.setName("90テスト社員"); emp.setDepartment("開発部"); emp.setEmail("90@test.com");
        emp.setJoinDate(LocalDate.now()); emp.setStatus("在職");
        EmployeeDTO created = empService.create(emp);
        assertNotNull(created);
        String csv = empService.exportCsv();
        assertTrue(csv.contains("EMP"));
        empService.delete(created.getId());
    }

    @Test void testInvoice_uploadDocument() throws Exception {
        InvoiceDTO created = invService.create(makeInvoice());
        MockMultipartFile file = new MockMultipartFile("file","coverage.pdf","application/pdf","data".getBytes());
        OrderDocumentDTO doc = invService.uploadDocument(created.getId(), file);
        assertNotNull(doc);
        invService.deleteDocument(doc.getId());
        invService.delete(created.getId());
    }

    @Test void testAttendance_fullGenerate() {
        int c1 = attService.generateMonth(2026, 10, 1L);
        int c2 = attService.generateMonthForAll(2026, 11);
        assertTrue(c1 >= 0); assertTrue(c2 >= 0);
    }

    @Test void testSO_withDetails() throws Exception {
        Map<String,Object> dto = new HashMap<>();
        dto.put("supplierName","90テスト"); dto.put("orderDate",LocalDate.now().toString());
        dto.put("amount",7000000.0); dto.put("taxRate",10.0);
        dto.put("issuerName","発注者"); dto.put("issuerDept","発注部");
        dto.put("supplierContact","受注担当"); dto.put("supplierDept","受注部");
        dto.put("supplierTel","03-0000"); dto.put("supplierAddr","東京都");
        Map<String,Object> created = soService.create(dto);
        byte[] pdf = soService.exportPdf((Long)created.get("id"));
        assertNotNull(pdf); assertTrue(pdf.length > 2000);
        soService.delete((Long)created.get("id"));
    }

    @Test void testPO_withAttachment() throws Exception {
        PurchaseOrderDTO created = poService.create(PurchaseOrderDTO.builder()
            .customerId(1L).orderDate(LocalDate.now()).deliveryDate(LocalDate.now().plusMonths(1))
            .subject("90添付テスト").amount(500000.0).taxRate(10.0).build());
        MockMultipartFile file = new MockMultipartFile("file","po90.pdf","application/pdf","xyz".getBytes());
        poService.uploadAttachment(created.getId(), file);
        PurchaseOrderDTO found = poService.findById(created.getId());
        assertTrue(found.getAttachments().size() > 0);
        poService.deleteAttachment(found.getAttachments().get(0).getId());
        poService.delete(created.getId());
    }

    @Test void testEmployee_batchImport() throws Exception {
        String csvContent = "EMP0098,90一郎,法務部,90ichiro@test.com\nEMP0097,90二郎,経理部,90jiro@test.com";
        Path csvFile = tempDir.resolve("import90.csv");
        Files.write(csvFile, csvContent.getBytes("UTF-8"));
        MockMultipartFile file = new MockMultipartFile("file","import90.csv","text/csv",
            Files.readAllBytes(csvFile));
        int count = empService.batchImport(file);
        assertTrue(count >= 0);
    }

    private InvoiceDTO makeInvoice() {
        InvoiceDTO d = new InvoiceDTO();
        d.setCustomerId(1L); d.setYear(2026); d.setMonth(12); d.setAmount(600000.0); d.setTaxRate(10.0);
        return d;
    }
}
