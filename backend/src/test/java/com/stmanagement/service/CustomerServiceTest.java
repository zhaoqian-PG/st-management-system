package com.stmanagement.service;

import com.stmanagement.dto.CustomerDTO;
import com.stmanagement.model.Customer;
import com.stmanagement.repository.BankAccountRepository;
import com.stmanagement.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
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
class CustomerServiceTest {

    @Mock private CustomerRepository repo;
    @Mock private BankAccountRepository baRepo;
    @Mock private EntityManager em;
    @InjectMocks private CustomerService service;

    private Customer c;

    @BeforeEach
    void setUp() {
        c = new Customer(); c.setId(1L); c.setCustomerCode("CUS0001");
        c.setCompanyName("テスト株式会社"); c.setPresidentName("山田 太郎");
        c.setEmail("test@example.com"); c.setPhone("03-1111-2222"); c.setTorihikiNo("BK000001");
    }

    @Test
    void testFindAll() {
        Page<Customer> page = new PageImpl<>(Collections.singletonList(c));
        when(repo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        Page<CustomerDTO> result = service.findAll(null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("CUS0001", result.getContent().get(0).getCustomerCode());
    }

    @Test
    void testFindById() {
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        CustomerDTO result = service.findById(1L);
        assertNotNull(result);
        assertEquals("テスト株式会社", result.getCompanyName());
    }

    @Test
    void testCreate() {
        CustomerDTO dto = new CustomerDTO();
        dto.setCompanyName("新規株式会社"); dto.setPresidentName("田中 宏");
        when(repo.save(any(Customer.class))).thenReturn(c);
        CustomerDTO result = service.create(dto);
        assertNotNull(result);
        verify(repo).save(any(Customer.class));
    }

    @Test
    void testUpdate() {
        CustomerDTO dto = new CustomerDTO();
        dto.setCompanyName("更新株式会社"); dto.setCustomerCode("CUS0001");
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(repo.save(any(Customer.class))).thenReturn(c);
        CustomerDTO result = service.update(1L, dto);
        assertNotNull(result);
        verify(repo).save(any(Customer.class));
    }

    @Test
    void testDelete() {
        when(repo.existsById(1L)).thenReturn(true);
        service.delete(1L);
        verify(repo).deleteById(1L);
    }

    @Test
    void testDelete_NotFound() {
        when(repo.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.delete(99L));
    }

    @Test
    void testFindById_NotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }
}
