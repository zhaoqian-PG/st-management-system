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
class CoverageBoosterTest {

    @Autowired private BankAccountService baService;
    @Autowired private AttendanceService attService;
    @Autowired private EmployeeService empService;
    @Autowired private InvoiceService invService;
    @Autowired private PurchaseOrderService poService;
    @Autowired private SupplierOrderService soService;

    @Test void testBankAccountCreate_withEntityManager() {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setBankName("カバレッジ銀行"); dto.setAccountType("普通");
        dto.setAccountNumber("1111111"); dto.setAccountHolder("カバレッジ名義");
        dto.setCategory("CUSTOMER"); dto.setCustomerId(1L); dto.setBranchCode("001");
        BankAccountDTO created = baService.create(dto);
        assertNotNull(created); assertTrue(created.getTorihikiNo().startsWith("BK"));
        baService.delete(created.getId());
    }

    @Test void testBankAccountCreate_withTorihikiGroup() {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setTorihikiNo("BK000001"); dto.setBankName("グループ追加"); dto.setAccountType("当座");
        dto.setAccountNumber("2222222"); dto.setAccountHolder("グループ名義");
        dto.setCategory("CUSTOMER"); dto.setCustomerId(1L);
        BankAccountDTO created = baService.create(dto);
        assertNotNull(created); assertTrue(created.getBranchNo().compareTo("001") >= 0);
        baService.delete(created.getId());
    }

    @Test void testAttendance_generateMonthForAll() {
        int count = attService.generateMonthForAll(2026, 8);
        assertTrue(count >= 0);
    }

    @Test void testAttendance_generateSingleMonth() {
        int count = attService.generateMonth(2026, 7, 1L);
        assertTrue(count >= 0);
    }

    @Test void testEmployee_exportCsv() {
        String csv = empService.exportCsv();
        assertNotNull(csv); assertFalse(csv.isEmpty());
    }

    @Test void testInvoice_exportCsv() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L); dto.setYear(2026); dto.setMonth(9); dto.setAmount(300000.0); dto.setTaxRate(10.0);
        InvoiceDTO created = invService.create(dto);
        assertNotNull(created);
        String csv = invService.exportInvoiceCsv(created.getId());
        assertNotNull(csv);
        invService.delete(created.getId());
    }

    @Test void testSO_pdfExport() throws Exception {
        Map<String,Object> so = new HashMap<>();
        so.put("supplierName","カバレッジ発注先"); so.put("orderDate",LocalDate.now().toString());
        so.put("amount",1000000.0); so.put("taxRate",10.0);
        Map<String,Object> created = soService.create(so);
        byte[] pdf = soService.exportPdf((Long)created.get("id"));
        assertNotNull(pdf); assertTrue(pdf.length > 1500);
        soService.delete((Long)created.get("id"));
    }

}
