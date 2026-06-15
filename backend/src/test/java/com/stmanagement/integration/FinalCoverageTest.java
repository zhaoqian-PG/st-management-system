package com.stmanagement.integration;

import com.stmanagement.dto.*;
import com.stmanagement.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class FinalCoverageTest {

    @Autowired private InvoiceService invService;
    @Autowired private EmployeeService empService;
    @Autowired private PurchaseOrderService poService;
    @Autowired private SupplierOrderService soService;
    @Autowired private BankAccountService baService;

    @Test void testInvoice_fullDocumentFlow() throws Exception {
        InvoiceDTO created = invService.create(makeInvoice());
        assertNotNull(created);

        MockMultipartFile file = new MockMultipartFile("file","請求書.pdf","application/pdf","testdata".getBytes());
        OrderDocumentDTO doc = invService.uploadDocument(created.getId(), file);
        assertNotNull(doc); assertEquals("請求書.pdf", doc.getFileName());

        String docName = invService.getDocumentFileName(doc.getId());
        assertEquals("請求書.pdf", docName);

        invService.deleteDocument(doc.getId());
        invService.delete(created.getId());
    }

    @Test void testInvoice_exportMultiple() {
        InvoiceDTO c1 = invService.create(makeInvoice());
        String csv1 = invService.exportInvoiceCsv(c1.getId());
        assertNotNull(csv1); assertTrue(csv1.length() > 0);
        invService.delete(c1.getId());

        InvoiceDTO c2 = invService.create(makeInvoice());
        String csv2 = invService.exportInvoiceCsv(c2.getId());
        assertNotNull(csv2);
        invService.delete(c2.getId());
    }

    @Test void testEmployee_uploadAndExport() throws Exception {
        EmployeeDTO emp = new EmployeeDTO();
        emp.setName("テスト太郎"); emp.setDepartment("総務部"); emp.setEmail("test@coverage.com");
        emp.setJoinDate(LocalDate.now()); emp.setStatus("在職");
        EmployeeDTO created = empService.create(emp);
        assertNotNull(created);

        String csv = empService.exportCsv();
        assertNotNull(csv); assertTrue(csv.contains("EMP"));

        empService.delete(created.getId());
    }

    @Test void testSO_multipleCreate() throws Exception {
        for (int i = 0; i < 3; i++) {
            Map<String,Object> so = new HashMap<>();
            so.put("supplierName","テスト"+i); so.put("orderDate",LocalDate.now().toString());
            so.put("amount",1000000.0+i*100000); so.put("taxRate",10.0);
            Map<String,Object> created = soService.create(so);
            assertNotNull(created);
            byte[] pdf = soService.exportPdf((Long)created.get("id"));
            assertNotNull(pdf);
            soService.delete((Long)created.get("id"));
        }
    }

    @Test void testPO_multipleCreate() {
        for (int i = 0; i < 3; i++) {
            PurchaseOrderDTO dto = PurchaseOrderDTO.builder().customerId(1L)
                .orderDate(LocalDate.now()).deliveryDate(LocalDate.now().plusMonths(1))
                .amount(500000.0+i*100000).taxRate(10.0).build();
            PurchaseOrderDTO created = poService.create(dto);
            assertNotNull(created);
            poService.delete(created.getId());
        }
    }

    @Test void testBankAccount_multipleFinds() {
        assertNotNull(baService.findByCustomerId(1L));
        assertNotNull(baService.findByEmployeeId(1L));
        assertNotNull(baService.getExistingTorihikiNos("CUSTOMER"));
    }

    private InvoiceDTO makeInvoice() {
        InvoiceDTO d = new InvoiceDTO();
        d.setCustomerId(1L); d.setYear(2026); d.setMonth(10); d.setAmount(500000.0); d.setTaxRate(10.0);
        return d;
    }
}
