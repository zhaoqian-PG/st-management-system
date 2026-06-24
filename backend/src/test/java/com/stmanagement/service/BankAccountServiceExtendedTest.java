package com.stmanagement.service;

import com.stmanagement.dto.BankAccountDTO;
import com.stmanagement.model.*;
import com.stmanagement.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceExtendedTest {
    @Mock private BankAccountRepository repo;
    @Mock private CustomerRepository custRepo;
    @Mock private EmployeeRepository empRepo;
    @Mock private EntityManager em;
    @InjectMocks private BankAccountService service;

    private BankAccount ba() {
        BankAccount b = new BankAccount(); b.setId(1L); b.setTorihikiNo("BK000001"); b.setBranchNo("001");
        b.setCategory("CUSTOMER"); b.setCustomerId(1L); b.setBankName("三菱UFJ");
        b.setAccountType("普通"); b.setAccountNumber("7654321"); b.setAccountHolder("テスト");
        b.setBranchCode("038"); b.setIsDefault(true);
        return b;
    }

    @Test void testFindAll_withCategoryFilter() {
        when(repo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(ba())));
        Page<BankAccountDTO> r = service.findAll("CUSTOMER", 1L, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testNextBranchNo_multiple() {
        BankAccount b1 = ba(); BankAccount b2 = ba(); b2.setBranchNo("002");
        when(repo.findByTorihikiNo(anyString())).thenReturn(Arrays.asList(b1, b2));
        String r = service.nextBranchNo("BK000001");
        assertEquals("003", r);
    }
    @Test void testFindUnlinkedByTorihikiNo() {
        BankAccount b = ba(); b.setCategory("CUSTOMER"); b.setCustomerId(null); b.setEmployeeId(null);
        when(repo.findByTorihikiNo("BK000001")).thenReturn(Collections.singletonList(b));
        List<BankAccountDTO> r = service.findUnlinkedByTorihikiNo("CUSTOMER", "BK000001");
        assertNotNull(r); assertEquals(1, r.size());
    }
    @Test void testBindToCustomer_withNewTorihiki() {
        when(repo.findById(1L)).thenReturn(Optional.of(ba()));
        Customer c = new Customer(); c.setId(2L); c.setTorihikiNo(null);
        when(custRepo.findById(2L)).thenReturn(Optional.of(c));
        when(repo.save(any(BankAccount.class))).thenReturn(ba());
        service.bindToCustomer(1L, 2L);
    }
    @Test void testBindToEmployee_withNewTorihiki() {
        when(repo.findById(1L)).thenReturn(Optional.of(ba()));
        Employee e = new Employee(); e.setId(2L); e.setTorihikiNo(null);
        when(empRepo.findById(2L)).thenReturn(Optional.of(e));
        when(repo.save(any(BankAccount.class))).thenReturn(ba());
        service.bindToEmployee(1L, 2L);
    }
    @Test void testFindAll_empty() {
        when(repo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        Page<BankAccountDTO> r = service.findAll(null, null, 0, 10);
        assertNotNull(r); assertTrue(r.isEmpty());
    }
    @Test void testNextBranchNo_first() {
        when(repo.findByTorihikiNo(anyString())).thenReturn(Collections.emptyList());
        assertEquals("001", service.nextBranchNo("BK000099"));
    }
}
