package com.stmanagement.service;

import com.stmanagement.dto.BankAccountDTO;
import com.stmanagement.model.BankAccount;
import com.stmanagement.model.Customer;
import com.stmanagement.model.Employee;
import com.stmanagement.repository.BankAccountRepository;
import com.stmanagement.repository.CustomerRepository;
import com.stmanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock private BankAccountRepository repo;
    @Mock private CustomerRepository customerRepo;
    @Mock private EmployeeRepository employeeRepo;
    @Mock private EntityManager em;
    @InjectMocks private BankAccountService service;

    private BankAccount ba() {
        BankAccount b = new BankAccount(); b.setId(1L); b.setTorihikiNo("BK000001"); b.setBranchNo("001");
        b.setCategory("CUSTOMER"); b.setCustomerId(1L); b.setBankName("三菱UFJ銀行");
        b.setAccountType("普通"); b.setAccountNumber("7654321"); b.setAccountHolder("テスト");
        b.setBranchCode("038"); b.setIsDefault(true);
        return b;
    }

    @Test void testFindAll() {
        when(repo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(ba())));
        Page<BankAccountDTO> r = service.findAll(null, null, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }

    @Test void testFindById() {
        when(repo.findById(1L)).thenReturn(Optional.of(ba()));
        BankAccountDTO r = service.findById(1L);
        assertNotNull(r); assertEquals("BK000001", r.getTorihikiNo());
    }

    @Test void testFindByCustomerId() {
        when(repo.findByCustomerId(1L)).thenReturn(Collections.singletonList(ba()));
        List<BankAccountDTO> r = service.findByCustomerId(1L);
        assertNotNull(r); assertEquals(1, r.size());
    }

    @Test void testFindByEmployeeId() {
        when(repo.findAll(any(Specification.class))).thenReturn(Collections.singletonList(ba()));
        List<BankAccountDTO> r = service.findByEmployeeId(1L);
        assertNotNull(r); assertEquals(1, r.size());
    }

    @Test void testNextBranchNo() {
        when(repo.findByTorihikiNo(anyString())).thenReturn(Collections.singletonList(ba()));
        String r = service.nextBranchNo("BK000001");
        assertNotNull(r);
    }

    @Test void testGetExistingTorihikiNos() {
        when(repo.findAll(any(Specification.class))).thenReturn(Collections.singletonList(ba()));
        List<String> r = service.getExistingTorihikiNos("CUSTOMER");
        assertNotNull(r); assertEquals(1, r.size());
    }

    @Test void testDelete() {
        when(repo.existsById(1L)).thenReturn(true);
        doNothing().when(repo).deleteById(1L);
        service.delete(1L);
        verify(repo).deleteById(1L);
    }

    @Test void testDelete_NotFound() {
        when(repo.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.delete(99L));
    }

    @Test void testBindToCustomer() {
        when(repo.findById(1L)).thenReturn(Optional.of(ba()));
        Customer c = new Customer(); c.setId(2L); c.setTorihikiNo("BK000099");
        when(customerRepo.findById(2L)).thenReturn(Optional.of(c));
        when(repo.save(any(BankAccount.class))).thenReturn(ba());
        service.bindToCustomer(1L, 2L);
        verify(repo).save(any(BankAccount.class));
    }

    @Test void testUnbindFromCustomer() {
        when(repo.findById(1L)).thenReturn(Optional.of(ba()));
        when(repo.save(any(BankAccount.class))).thenReturn(ba());
        service.unbindFromCustomer(1L);
        verify(repo).save(any(BankAccount.class));
    }

    @Test void testBindToEmployee() {
        when(repo.findById(1L)).thenReturn(Optional.of(ba()));
        Employee e = new Employee(); e.setId(2L); e.setTorihikiNo("BK000099");
        when(employeeRepo.findById(2L)).thenReturn(Optional.of(e));
        when(repo.save(any(BankAccount.class))).thenReturn(ba());
        service.bindToEmployee(1L, 2L);
        verify(repo).save(any(BankAccount.class));
    }

    @Test void testCreate() {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setBankName("テスト"); dto.setAccountType("普通"); dto.setAccountNumber("123");
        dto.setAccountHolder("名義"); dto.setCategory("CUSTOMER"); dto.setCustomerId(1L);
        when(customerRepo.findById(1L)).thenReturn(Optional.empty());
        Query q = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(q);
        when(q.getSingleResult()).thenReturn(BigInteger.valueOf(200));
        when(em.merge(any(BankAccount.class))).thenReturn(ba());
        when(repo.save(any(BankAccount.class))).thenReturn(ba());
        BankAccountDTO r = service.create(dto);
        assertNotNull(r);
    }

    @Test void testUpdate() {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setBankName("更新"); dto.setCategory("CUSTOMER");
        when(repo.findById(1L)).thenReturn(Optional.of(ba()));
        when(repo.save(any(BankAccount.class))).thenReturn(ba());
        BankAccountDTO r = service.update(1L, dto);
        assertNotNull(r);
    }
}
