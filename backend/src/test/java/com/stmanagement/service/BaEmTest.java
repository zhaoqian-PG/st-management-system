package com.stmanagement.service;

import com.stmanagement.dto.BankAccountDTO;
import com.stmanagement.model.*;
import com.stmanagement.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
class BaEmTest {
    @Mock private BankAccountRepository repo;
    @Mock private CustomerRepository cRepo;
    @Mock private EmployeeRepository eRepo;
    @Mock private EntityManager em;
    @Mock private Query query;
    @InjectMocks private BankAccountService svc;

    @Test void create_newTorihiki_usesEntityManager() {
        BankAccountDTO d = dto("新規");
        when(em.createNativeQuery(contains("nextval"))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(BigInteger.valueOf(700L));
        when(em.merge(any())).thenAnswer(i -> i.getArgument(0));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cRepo.findById(1L)).thenReturn(Optional.empty());
        BankAccountDTO r = svc.create(d);
        assertEquals("BK000700", r.getTorihikiNo());
        assertEquals("001", r.getBranchNo());
    }

    @Test void create_existingTorihiki_calcBranch() {
        BankAccountDTO d = dto("枝番"); d.setTorihikiNo("BK000001");
        BankAccount b1 = ba("BK000001","001"); BankAccount b2 = ba("BK000001","005");
        when(repo.findByTorihikiNo("BK000001")).thenReturn(Arrays.asList(b1,b2));
        when(em.merge(any())).thenAnswer(i -> i.getArgument(0));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cRepo.findById(1L)).thenReturn(Optional.empty());
        assertEquals("006", svc.create(d).getBranchNo());
    }

    @Test void create_firstBranch() {
        BankAccountDTO d = dto("最初"); d.setTorihikiNo("BK000099");
        when(repo.findByTorihikiNo("BK000099")).thenReturn(Collections.emptyList());
        when(em.merge(any())).thenAnswer(i -> i.getArgument(0));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cRepo.findById(1L)).thenReturn(Optional.empty());
        assertEquals("001", svc.create(d).getBranchNo());
    }

    @Test void bindToCustomer_assignsTorihiki() {
        BankAccount b = new BankAccount(); b.setId(1L); b.setTorihikiNo(null);
        when(repo.findById(1L)).thenReturn(Optional.of(b));
        Customer c = new Customer(); c.setId(2L); c.setTorihikiNo("BK000099");
        when(cRepo.findById(2L)).thenReturn(Optional.of(c));
        when(repo.save(any())).thenReturn(b);
        svc.bindToCustomer(1L, 2L);
        verify(repo).save(any());
    }

    @Test void bindToEmployee_assignsTorihiki() {
        BankAccount b = new BankAccount(); b.setId(1L); b.setTorihikiNo(null);
        when(repo.findById(1L)).thenReturn(Optional.of(b));
        Employee e = new Employee(); e.setId(2L); e.setTorihikiNo("BK000088");
        when(eRepo.findById(2L)).thenReturn(Optional.of(e));
        when(repo.save(any())).thenReturn(b);
        svc.bindToEmployee(1L, 2L);
        verify(repo).save(any());
    }

    @Test void unbindFromCustomer() {
        when(repo.findById(1L)).thenReturn(Optional.of(ba("BK000001","001")));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        svc.unbindFromCustomer(1L);
        verify(repo).save(any());
    }

    @Test void unbindFromEmployee() {
        when(repo.findById(1L)).thenReturn(Optional.of(ba("BK000001","001")));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        svc.unbindFromEmployee(1L);
        verify(repo).save(any());
    }

    @Test void setDefaultForCustomer() {
        BankAccount b = ba("BK000001","001"); b.setIsDefault(false); b.setCustomerId(2L);
        when(repo.findByCustomerId(2L)).thenReturn(Collections.singletonList(b));
        when(repo.save(any())).thenReturn(b);
        svc.setDefaultForCustomer(1L, 2L);
        verify(repo, atLeastOnce()).save(any());
    }

    @Test void setDefaultForEmployee() {
        BankAccount b = ba("BK000001","001"); b.setIsDefault(false); b.setEmployeeId(2L);
        when(repo.findAll(any(Specification.class))).thenReturn(Collections.singletonList(b));
        when(repo.save(any())).thenReturn(b);
        svc.setDefaultForEmployee(1L, 2L);
        verify(repo, atLeastOnce()).save(any());
    }

    @Test void findByEmployeeId() {
        when(repo.findAll(any(Specification.class))).thenReturn(Collections.singletonList(ba("BK000001","001")));
        assertEquals(1, svc.findByEmployeeId(1L).size());
    }

    @Test void getExistingTorihikiNos() {
        when(repo.findAll(any(Specification.class))).thenReturn(Collections.singletonList(ba("BK000001","001")));
        assertEquals(1, svc.getExistingTorihikiNos("CUSTOMER").size());
    }

    @Test void nextBranchNo_multi() {
        when(repo.findByTorihikiNo("BK000001")).thenReturn(Arrays.asList(ba("BK000001","001"),ba("BK000001","003")));
        assertEquals("004", svc.nextBranchNo("BK000001"));
    }

    @Test void nextBranchNo_first() {
        when(repo.findByTorihikiNo("BK000099")).thenReturn(Collections.emptyList());
        assertEquals("001", svc.nextBranchNo("BK000099"));
    }

    @Test void findUnlinkedByTorihiki() {
        BankAccount b = ba("BK000099","001"); b.setCustomerId(null); b.setEmployeeId(null);
        when(repo.findByTorihikiNo("BK000099")).thenReturn(Collections.singletonList(b));
        assertEquals(1, svc.findUnlinkedByTorihikiNo("CUSTOMER","BK000099").size());
    }

    @Test void update_success() {
        BankAccountDTO d = dto("更新"); d.setCategory("CUSTOMER");
        when(repo.findById(1L)).thenReturn(Optional.of(ba("BK000001","001")));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("更新", svc.update(1L, d).getBankName());
    }

    @Test void findAll_filtered() {
        when(repo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(ba("BK000001","001"))));
        assertEquals(1, svc.findAll("CUSTOMER", 1L, 0, 10).getTotalElements());
    }

    private BankAccountDTO dto(String n) {
        BankAccountDTO d = new BankAccountDTO();
        d.setBankName(n); d.setAccountType("普通"); d.setAccountNumber("123");
        d.setAccountHolder("名義"); d.setCategory("CUSTOMER"); d.setCustomerId(1L);
        return d;
    }
    private BankAccount ba(String t, String b) {
        BankAccount a = new BankAccount();
        a.setId(1L); a.setTorihikiNo(t); a.setBranchNo(b); a.setCategory("CUSTOMER");
        a.setCustomerId(1L); a.setBankName("銀行"); a.setAccountType("普通");
        a.setAccountNumber("123"); a.setAccountHolder("名義");
        return a;
    }
}
