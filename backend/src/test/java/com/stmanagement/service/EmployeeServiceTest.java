package com.stmanagement.service;

import com.stmanagement.dto.EmployeeDTO;
import com.stmanagement.model.Employee;
import com.stmanagement.repository.BankAccountRepository;
import com.stmanagement.repository.EmployeeAttachmentRepository;
import com.stmanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepo;
    @Mock private EmployeeAttachmentRepository attRepo;
    @Mock private BankAccountRepository baRepo;
    @Mock private EntityManager em;
    @InjectMocks private EmployeeService service;

    private Employee emp;

    @BeforeEach
    void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setEmployeeCode("EMP0001");
        emp.setName("山田 太郎"); emp.setDepartment("営業部");
        emp.setEmail("yamada@example.com"); emp.setPhone("090-1111-2222");
        emp.setStatus("在職"); emp.setJoinDate(LocalDate.of(2020, 4, 1));
    }

    @Test
    void testFindAll() {
        when(employeeRepo.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(emp)));

        Page<EmployeeDTO> result = service.findAll(null, null, false, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testFindById() {
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));
        EmployeeDTO result = service.findById(1L);
        assertNotNull(result);
        assertEquals("山田 太郎", result.getName());
    }

    @Test
    void testFindById_NotFound() {
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }

    @Test
    void testCreate() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("新規社員"); dto.setDepartment("技術部");
        when(employeeRepo.save(any(Employee.class))).thenReturn(emp);
        EmployeeDTO result = service.create(dto);
        assertNotNull(result);
    }

    @Test
    void testUpdate() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("更新太郎"); dto.setDepartment("経理部");
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));
        when(employeeRepo.save(any(Employee.class))).thenReturn(emp);
        EmployeeDTO result = service.update(1L, dto);
        assertNotNull(result);
    }

    @Test
    void testDelete() {
        when(employeeRepo.existsById(1L)).thenReturn(true);
        doNothing().when(employeeRepo).deleteById(1L);
        service.delete(1L);
        verify(employeeRepo).deleteById(1L);
    }
}
