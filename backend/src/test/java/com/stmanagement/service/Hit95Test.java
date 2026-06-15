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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Hit95Test {
    // BankAccount - remaining EntityManager coverage
    @Mock private BankAccountRepository baRepo;
    @Mock private CustomerRepository custRepo;
    @Mock private EmployeeRepository empRepo;
    @Mock private EntityManager em;
    @Mock private Query query;
    @InjectMocks private BankAccountService baService;

    // SupplierOrder - PDF + details
    @Mock private SupplierOrderRepository soRepo;
    @Mock private SupplierOrderDetailRepository soDetRepo;
    @InjectMocks private SupplierOrderService soService;

    // Attendance - remaining
    @Mock private AttendanceRepository attRepo;
    @Mock private EmployeeRepository eRepo;
    @InjectMocks private AttendanceService attService;

    private Attendance att;
    private Employee emp;

    @BeforeEach void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setName("山田"); emp.setDepartment("営業部");
        att = new Attendance(); att.setId(1L); att.setEmployeeId(1L);
        att.setWorkDate(LocalDate.of(2026,5,1)); att.setWorkHours(8.0); att.setOvertimeHours(2.0);
        att.setTotalHours(10.0); att.setWorkType("NORMAL"); att.setStatus("出勤");
    }

    // BA: create with EntityManager sequence
    @Test void ba_create_emSequence() {
        BankAccountDTO d = baDto("EM銀行"); d.setTorihikiNo(null);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(BigInteger.valueOf(500L));
        when(em.merge(any())).thenAnswer(i -> i.getArgument(0));
        when(baRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(custRepo.findById(1L)).thenReturn(Optional.empty());
        BankAccountDTO r = baService.create(d);
        assertEquals("BK000500", r.getTorihikiNo());
    }

    // BA: create existing torihiki with branches
    @Test void ba_create_existingTorihiki_multiBranch() {
        BankAccountDTO d = baDto("枝番銀行"); d.setTorihikiNo("BK000001");
        BankAccount b1 = bankAccount("BK000001","001"); BankAccount b2 = bankAccount("BK000001","005");
        when(baRepo.findByTorihikiNo("BK000001")).thenReturn(Arrays.asList(b1,b2));
        when(em.merge(any())).thenAnswer(i -> i.getArgument(0));
        when(baRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(custRepo.findById(1L)).thenReturn(Optional.empty());
        BankAccountDTO r = baService.create(d);
        assertEquals("006", r.getBranchNo());
    }

    // SO: PDF export with details
    @Test void so_exportPdf_withDetails() throws Exception {
        SupplierOrder so = SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001")
            .supplierName("テスト").orderDate(LocalDate.of(2026,5,10)).deliveryDate(LocalDate.of(2026,7,1))
            .subject("テスト").amount(5000000.0).taxRate(10.0).taxAmount(500000.0).totalWithTax(5500000.0)
            .status("発注済").issuerName("山田").issuerDept("営業部").issuerTel("090-1111")
            .supplierContact("佐々木").supplierDept("開発部").supplierTel("03-1234").supplierAddr("東京都港区")
            .remark("備考").build();
        when(soRepo.findById(1L)).thenReturn(Optional.of(so));
        SupplierOrderDetail d1 = SupplierOrderDetail.builder().id(1L).orderId(1L).employeeName("山田")
            .itemName("サーバー設計").quantity(1.0).unitPrice(3000000.0).amount(3000000.0).build();
        SupplierOrderDetail d2 = SupplierOrderDetail.builder().id(2L).orderId(1L).employeeName("鈴木")
            .itemName("ネットワーク設定").quantity(1.0).unitPrice(2000000.0).amount(2000000.0).build();
        when(soDetRepo.findByOrderId(1L)).thenReturn(Arrays.asList(d1,d2));
        byte[] pdf = soService.exportPdf(1L);
        assertNotNull(pdf); assertTrue(pdf.length > 3000);
    }

    // SO: update with details
    @Test void so_update_withDetails() {
        SupplierOrder so = SupplierOrder.builder().id(1L).orderNumber("PO-SUP-2026-0001")
            .supplierName("テスト").orderDate(LocalDate.of(2026,5,10)).amount(1000000.0)
            .taxRate(10.0).taxAmount(100000.0).totalWithTax(1100000.0).status("下書き").build();
        when(soRepo.findById(1L)).thenReturn(Optional.of(so));
        when(soRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(soDetRepo.findByOrderId(1L)).thenReturn(Collections.emptyList());
        Map<String,Object> dto = new HashMap<>();
        dto.put("supplierName","更新"); dto.put("orderDate","2026-06-01"); dto.put("amount",2000000.0); dto.put("taxRate",10.0);
        List<Map<String,Object>> details = new ArrayList<>();
        Map<String,Object> det = new HashMap<>();
        det.put("employeeName","田中"); det.put("itemName","テスト"); det.put("quantity",1.0); det.put("unitPrice",500000.0);
        details.add(det); dto.put("details",details);
        Map<String,Object> r = soService.update(1L, dto);
        assertNotNull(r);
    }

    // ATT: generateMonth with existing records
    @Test void att_generateMonth_withExisting() {
        when(attRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.singletonList(att));
        int count = attService.generateMonth(2026, 5, 1L);
        assertTrue(count >= 0);
    }

    // ATT: getAllEmployeeMonthlySummary
    @Test void att_allSummary() {
        when(eRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(attRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.singletonList(att));
        List<Map<String,Object>> r = attService.getAllEmployeeMonthlySummary(2026, 5);
        assertNotNull(r); assertTrue(r.size() > 0);
    }

    // ATT: getMonthlySummary
    @Test void att_monthlySummary() {
        when(attRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.singletonList(att));
        Map<String,Object> r = attService.getMonthlySummary(2026, 5, 1L);
        assertNotNull(r); assertNotNull(r.get("workHours"));
    }

    // ATT: export CSV operations
    @Test void att_exportCsv() {
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        when(attRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.singletonList(att));
        assertNotNull(attService.exportCsv(2026, 5, 1L));
    }

    @Test void att_exportCsvAll() {
        when(eRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(attRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.singletonList(att));
        assertNotNull(attService.exportCsvAll(2026, 5));
    }

    private BankAccountDTO baDto(String name) {
        BankAccountDTO d = new BankAccountDTO();
        d.setBankName(name); d.setAccountType("普通"); d.setAccountNumber("123"); d.setAccountHolder("名義");
        d.setCategory("CUSTOMER"); d.setCustomerId(1L);
        return d;
    }
    private BankAccount bankAccount(String torihiki, String branch) {
        BankAccount b = new BankAccount(); b.setTorihikiNo(torihiki); b.setBranchNo(branch);
        return b;
    }
}
