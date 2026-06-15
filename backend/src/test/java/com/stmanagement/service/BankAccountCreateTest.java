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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountCreateTest {
    @Mock private BankAccountRepository repo;
    @Mock private CustomerRepository custRepo;
    @Mock private EmployeeRepository empRepo;
    @Mock private EntityManager em;
    @Mock private Query query;
    @InjectMocks private BankAccountService service;

    @Test void testCreate_withEntityManager() {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setBankName("新規銀行"); dto.setAccountType("普通"); dto.setAccountNumber("1234567");
        dto.setAccountHolder("テスト名義"); dto.setCategory("CUSTOMER"); dto.setCustomerId(1L);

        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(BigInteger.valueOf(200L));
        when(em.merge(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(repo.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(custRepo.findById(1L)).thenReturn(Optional.empty());

        BankAccountDTO result = service.create(dto);
        assertNotNull(result);
        assertEquals("BK000200", result.getTorihikiNo());
        assertEquals("001", result.getBranchNo());
    }

    @Test void testCreate_withExistingTorihiki() {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setTorihikiNo("BK000001"); dto.setBankName("既存グループ"); dto.setAccountType("当座");
        dto.setAccountNumber("9999999"); dto.setAccountHolder("名義"); dto.setCategory("CUSTOMER"); dto.setCustomerId(1L);

        when(custRepo.findById(1L)).thenReturn(Optional.empty());
        when(repo.findByTorihikiNo("BK000001")).thenReturn(java.util.Collections.emptyList());
        when(em.merge(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(repo.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));

        BankAccountDTO result = service.create(dto);
        assertNotNull(result);
        assertEquals("BK000001", result.getTorihikiNo());
    }
}
