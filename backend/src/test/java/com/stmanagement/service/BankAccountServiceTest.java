package com.stmanagement.service;

import com.stmanagement.dto.BankAccountDTO;
import com.stmanagement.model.BankAccount;
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
    @Mock private EntityManager entityManager;
    @InjectMocks private BankAccountService service;

    @Test
    void testFindById() {
        BankAccount ba = makeBankAccount();
        when(repo.findById(1L)).thenReturn(Optional.of(ba));
        BankAccountDTO result = service.findById(1L);
        assertNotNull(result);
        assertEquals("BK000001", result.getTorihikiNo());
    }

    @Test
    void testFindByCustomerId() {
        BankAccount ba = makeBankAccount();
        when(repo.findByCustomerId(1L)).thenReturn(Collections.singletonList(ba));
        List<BankAccountDTO> result = service.findByCustomerId(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testDelete() {
        when(repo.existsById(1L)).thenReturn(true);
        doNothing().when(repo).deleteById(1L);
        service.delete(1L);
        verify(repo).deleteById(1L);
    }

    @Test
    void testDelete_NotFound() {
        when(repo.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.delete(99L));
    }

    private BankAccount makeBankAccount() {
        BankAccount ba = new BankAccount();
        ba.setId(1L); ba.setTorihikiNo("BK000001"); ba.setBranchNo("001");
        ba.setCategory("CUSTOMER"); ba.setCustomerId(1L); ba.setBankName("三菱UFJ銀行");
        ba.setAccountType("普通"); ba.setAccountNumber("7654321"); ba.setAccountHolder("テスト名義");
        ba.setBranchCode("038"); ba.setIsDefault(true);
        return ba;
    }
}
