package com.stmanagement.service;

import com.stmanagement.dto.EmployeeDTO;
import com.stmanagement.model.*;
import com.stmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceExtendedTest {
    @Mock private EmployeeRepository empRepo;
    @Mock private EmployeeAttachmentRepository attRepo;
    @Mock private BankAccountRepository baRepo;
    @Mock private EntityManager em;
    @InjectMocks private EmployeeService service;
    private Employee emp;
    @BeforeEach void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setEmployeeCode("EMP0001");
        emp.setName("山田 太郎"); emp.setDepartment("営業部"); emp.setPosition("課長");
        emp.setEmail("yamada@test.com"); emp.setPhone("090-1111-2222");
        emp.setJapanAddress("東京都新宿区"); emp.setStatus("在職");
        emp.setJoinDate(LocalDate.of(2020,4,1));
    }

    @Test void testFindAll_withKeyword() {
        when(empRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        Page<EmployeeDTO> r = service.findAll("山田", "営業部", true, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testFindAll_empty() {
        when(empRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        Page<EmployeeDTO> r = service.findAll(null, null, false, 0, 10);
        assertNotNull(r); assertTrue(r.isEmpty());
    }
    @Test void testUpdate_withAllFields() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("更新太郎"); dto.setDepartment("経理部"); dto.setPosition("部長");
        dto.setEmail("update@test.com"); dto.setPhone("090-9999"); dto.setJapanAddress("東京都港区");
        dto.setStatus("在職"); dto.setJoinDate(LocalDate.of(2021,4,1));
        when(empRepo.findById(1L)).thenReturn(Optional.of(emp));
        when(empRepo.save(any(Employee.class))).thenReturn(emp);
        EmployeeDTO r = service.update(1L, dto);
        assertNotNull(r);
    }
    @Test void testDelete_byId() {
        when(empRepo.existsById(1L)).thenReturn(true);
        doNothing().when(empRepo).deleteById(1L);
        service.delete(1L);
        verify(empRepo).deleteById(1L);
    }
}
