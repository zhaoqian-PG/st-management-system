package com.stmanagement.service;

import com.stmanagement.dto.CustomerDTO;
import com.stmanagement.model.Customer;
import com.stmanagement.repository.BankAccountRepository;
import com.stmanagement.repository.CustomerRepository;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerFullCoverageTest {
    @Mock private CustomerRepository repo;
    @Mock private BankAccountRepository baRepo;
    @Mock private EntityManager em;
    @Mock private Query query;
    @InjectMocks private CustomerService service;
    private Customer c;

    @BeforeEach void setUp() {
        c = new Customer(); c.setId(1L); c.setCustomerCode("CUS0001"); c.setCompanyName("テスト");
        c.setPresidentName("社長"); c.setEmail("t@t.com"); c.setPhone("03-1111");
        c.setAddress("東京都"); c.setTorihikiNo("BK000001");
    }

    @Test void findAll_withKeyword() {
        when(repo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(c)));
        Page<CustomerDTO> r = service.findAll("テスト", 0, 10);
        assertEquals(1, r.getTotalElements()); assertEquals("CUS0001", r.getContent().get(0).getCustomerCode());
    }

    @Test void findAll_empty() {
        when(repo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        assertTrue(service.findAll(null, 0, 10).isEmpty());
    }

    @Test void findById_full() {
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        CustomerDTO r = service.findById(1L);
        assertEquals("テスト", r.getCompanyName());
        assertEquals("BK000001", r.getTorihikiNo());
    }

    @Test void create_newCustomer() {
        CustomerDTO dto = new CustomerDTO();
        dto.setCompanyName("新規"); dto.setPresidentName("新社長"); dto.setEmail("new@t.com");
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        CustomerDTO r = service.create(dto);
        assertNotNull(r); assertTrue(r.getCustomerCode().startsWith("CUS"));
    }

    @Test void update_allFields() {
        CustomerDTO dto = new CustomerDTO();
        dto.setCompanyName("更新"); dto.setPresidentName("更新社長"); dto.setCustomerCode("CUS0001");
        dto.setEmail("up@t.com"); dto.setPhone("090-9999"); dto.setAddress("大阪");
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        CustomerDTO r = service.update(1L, dto);
        assertEquals("更新", r.getCompanyName());
    }

    @Test void delete_success() {
        when(repo.existsById(1L)).thenReturn(true);
        doNothing().when(repo).deleteById(1L);
        service.delete(1L);
        verify(repo).deleteById(1L);
    }

    @Test void delete_notFound() {
        when(repo.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.delete(99L));
    }

    @Test void findById_notFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }
}
