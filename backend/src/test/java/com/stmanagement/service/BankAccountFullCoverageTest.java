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
class BankAccountFullCoverageTest {
    @Mock private BankAccountRepository repo;
    @Mock private CustomerRepository custRepo;
    @Mock private EmployeeRepository empRepo;
    @Mock private EntityManager em;
    @Mock private Query query;
    @InjectMocks private BankAccountService service;

    private BankAccount ba() {
        BankAccount b = new BankAccount(); b.setId(1L); b.setTorihikiNo("BK000001"); b.setBranchNo("001");
        b.setCategory("CUSTOMER"); b.setCustomerId(1L); b.setBankName("テスト"); b.setAccountType("普通");
        b.setAccountNumber("123"); b.setAccountHolder("名義"); b.setBranchCode("038"); b.setIsDefault(true);
        return b;
    }

    @Test void create_newTorihiki_usesEntityManager() {
        BankAccountDTO dto = dto("新規EM銀行");
        when(em.createNativeQuery(contains("nextval"))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(BigInteger.valueOf(300L));
        when(em.merge(any())).thenAnswer(i -> i.getArgument(0));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(custRepo.findById(1L)).thenReturn(Optional.empty());
        BankAccountDTO r = service.create(dto);
        assertEquals("BK000300", r.getTorihikiNo()); assertEquals("001", r.getBranchNo());
    }

    @Test void create_existingTorihiki_calculatesBranch() {
        BankAccountDTO dto = dto("既存EM銀行"); dto.setTorihikiNo("BK000001");
        BankAccount existing = ba(); existing.setBranchNo("003");
        when(repo.findByTorihikiNo("BK000001")).thenReturn(Collections.singletonList(existing));
        when(em.merge(any())).thenAnswer(i -> i.getArgument(0));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(custRepo.findById(1L)).thenReturn(Optional.empty());
        BankAccountDTO r = service.create(dto);
        assertEquals("004", r.getBranchNo());
    }

    @Test void findAll_withCategoryAndCustomer() {
        when(repo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(ba())));
        Page<BankAccountDTO> r = service.findAll("CUSTOMER", 1L, 0, 10);
        assertEquals(1, r.getTotalElements());
    }

    @Test void findByEmployeeId_usesSpecification() {
        when(repo.findAll(any(Specification.class))).thenReturn(Collections.singletonList(ba()));
        List<BankAccountDTO> r = service.findByEmployeeId(1L);
        assertEquals(1, r.size());
    }

    @Test void getExistingTorihikiNos_usesSpecification() {
        when(repo.findAll(any(Specification.class))).thenReturn(Collections.singletonList(ba()));
        List<String> r = service.getExistingTorihikiNos("EMPLOYEE");
        assertEquals(1, r.size());
    }

    @Test void findUnlinkedByTorihiki() {
        BankAccount b = ba(); b.setCustomerId(null); b.setEmployeeId(null);
        when(repo.findByTorihikiNo("BK000099")).thenReturn(Collections.singletonList(b));
        List<BankAccountDTO> r = service.findUnlinkedByTorihikiNo("CUSTOMER", "BK000099");
        assertEquals(1, r.size());
    }

    @Test void nextBranchNo_multipleExisting() {
        BankAccount b1 = ba(); b1.setBranchNo("001");
        BankAccount b2 = ba(); b2.setBranchNo("003");
        when(repo.findByTorihikiNo("BK000001")).thenReturn(Arrays.asList(b1, b2));
        assertEquals("004", service.nextBranchNo("BK000001"));
    }

    @Test void nextBranchNo_firstBranch() {
        when(repo.findByTorihikiNo("BK000099")).thenReturn(Collections.emptyList());
        assertEquals("001", service.nextBranchNo("BK000099"));
    }

    @Test void bindToCustomer_assignsTorihiki() {
        BankAccount b = ba(); b.setTorihikiNo(null);
        when(repo.findById(1L)).thenReturn(Optional.of(b));
        Customer c = new Customer(); c.setId(2L); c.setTorihikiNo("BK000099");
        when(custRepo.findById(2L)).thenReturn(Optional.of(c));
        when(repo.save(any())).thenReturn(b);
        service.bindToCustomer(1L, 2L);
        verify(repo).save(any());
    }

    @Test void bindToEmployee_assignsTorihiki() {
        BankAccount b = ba(); b.setTorihikiNo(null);
        when(repo.findById(1L)).thenReturn(Optional.of(b));
        Employee e = new Employee(); e.setId(2L); e.setTorihikiNo("BK000088");
        when(empRepo.findById(2L)).thenReturn(Optional.of(e));
        when(repo.save(any())).thenReturn(b);
        service.bindToEmployee(1L, 2L);
        verify(repo).save(any());
    }

    @Test void unbindFromCustomer() {
        when(repo.findById(1L)).thenReturn(Optional.of(ba()));
        when(repo.save(any())).thenReturn(ba());
        service.unbindFromCustomer(1L);
        verify(repo).save(any());
    }

    @Test void unbindFromEmployee() {
        when(repo.findById(1L)).thenReturn(Optional.of(ba()));
        when(repo.save(any())).thenReturn(ba());
        service.unbindFromEmployee(1L);
        verify(repo).save(any());
    }

    @Test void setDefaultForCustomer() {
        BankAccount b = ba(); b.setIsDefault(false); b.setCustomerId(2L);
        when(repo.findByCustomerId(2L)).thenReturn(Collections.singletonList(b));
        when(repo.save(any())).thenReturn(b);
        service.setDefaultForCustomer(1L, 2L);
        verify(repo, atLeastOnce()).save(any());
    }

    @Test void setDefaultForEmployee() {
        BankAccount b = ba(); b.setIsDefault(false); b.setEmployeeId(2L);
        when(repo.findAll(any(Specification.class))).thenReturn(Collections.singletonList(b));
        when(repo.save(any())).thenReturn(b);
        service.setDefaultForEmployee(1L, 2L);
        verify(repo, atLeastOnce()).save(any());
    }

    @Test void update_changesFields() {
        BankAccountDTO dto = dto("更新銀行");
        when(repo.findById(1L)).thenReturn(Optional.of(ba()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        BankAccountDTO r = service.update(1L, dto);
        assertEquals("更新銀行", r.getBankName());
    }

    @Test void delete_notFound() {
        when(repo.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.delete(99L));
    }

    private BankAccountDTO dto(String name) {
        BankAccountDTO d = new BankAccountDTO();
        d.setBankName(name); d.setAccountType("普通"); d.setAccountNumber("1234567");
        d.setAccountHolder("名義"); d.setCategory("CUSTOMER"); d.setCustomerId(1L);
        return d;
    }
}
