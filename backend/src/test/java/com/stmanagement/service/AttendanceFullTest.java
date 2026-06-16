package com.stmanagement.service;

import com.stmanagement.dto.AttendanceDTO;
import com.stmanagement.model.Attendance;
import com.stmanagement.model.Employee;
import com.stmanagement.repository.AttendanceRepository;
import com.stmanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceFullTest {
    @Mock private AttendanceRepository aRepo;
    @Mock private EmployeeRepository eRepo;
    @InjectMocks private AttendanceService service;

    private Attendance att;
    private Employee emp;

    @BeforeEach void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setName("山田"); emp.setDepartment("営業部");
        emp.setLeaveDate(null);
        att = new Attendance(); att.setId(1L); att.setEmployeeId(1L);
        att.setWorkDate(LocalDate.of(2026,5,1)); att.setWorkHours(8.0);
        att.setOvertimeHours(2.0); att.setTotalHours(10.0);
        att.setWorkType("NORMAL"); att.setStatus("出勤");
    }

    @Test void findAll_basic() {
        when(aRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(att)));
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        assertEquals(1, service.findAll(null,null,null,0,10).getTotalElements());
    }
    @Test void findAll_withFilters() {
        when(aRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(att)));
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        assertEquals(1, service.findAll(2026,5,1L,0,10).getTotalElements());
    }
    @Test void findAll_empty() {
        when(aRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        assertTrue(service.findAll(null,null,null,0,10).isEmpty());
    }
    @Test void findById_found() {
        when(aRepo.findById(1L)).thenReturn(Optional.of(att));
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        assertEquals(8.0, service.findById(1L).getWorkHours());
    }
    @Test void findById_notFound() {
        when(aRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }
    @Test void create_basic() {
        AttendanceDTO d = dto(); d.setWorkType("NORMAL"); d.setStatus("出勤");
        when(aRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.emptyList());
        when(aRepo.save(any())).thenReturn(att); when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        assertNotNull(service.create(d));
    }
    @Test void create_withOvertime() {
        AttendanceDTO d = dto(); d.setWorkHours(8.0); d.setOvertimeHours(3.0); d.setWorkType("NORMAL"); d.setStatus("出勤");
        when(aRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.emptyList());
        when(aRepo.save(any())).thenReturn(att); when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        assertNotNull(service.create(d));
    }
    @Test void create_withClockInOut() {
        AttendanceDTO d = dto(); d.setClockIn("09:00"); d.setClockOut("18:00"); d.setWorkType("NORMAL"); d.setStatus("出勤");
        when(aRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.emptyList());
        when(aRepo.save(any())).thenReturn(att); when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        assertNotNull(service.create(d));
    }
    @Test void update_basic() {
        AttendanceDTO d = dto(); d.setWorkHours(7.0); d.setWorkType("REMOTE"); d.setStatus("出勤");
        when(aRepo.findById(1L)).thenReturn(Optional.of(att)); when(aRepo.save(any())).thenReturn(att);
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        assertNotNull(service.update(1L, d));
    }
    @Test void delete_ok() { doNothing().when(aRepo).deleteById(1L); service.delete(1L); verify(aRepo).deleteById(1L); }
    @Test void getMonthlySummary() {
        when(aRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.singletonList(att));
        assertNotNull(service.getMonthlySummary(2026,5,1L));
    }
    @Test void getAllEmployeeMonthlySummary() {
        when(eRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(aRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.singletonList(att));
        assertEquals(1, service.getAllEmployeeMonthlySummary(2026,5).size());
    }
    @Test void generateMonth_allNew() {
        when(aRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.emptyList());
        when(aRepo.save(any())).thenReturn(att);
        assertTrue(service.generateMonth(2026,5,1L) > 0);
    }
    @Test void generateMonth_someExisting() {
        when(aRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.singletonList(att));
        assertTrue(service.generateMonth(2026,5,1L) >= 0);
    }
    @Test void generateMonth_nullParams() {
        assertEquals(0, service.generateMonth(null,5,1L));
        assertEquals(0, service.generateMonth(2026,null,1L));
        assertEquals(0, service.generateMonth(2026,5,null));
    }
    @Test void generateMonthForAll() {
        when(eRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(aRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.emptyList());
        when(aRepo.save(any())).thenReturn(att);
        assertTrue(service.generateMonthForAll(2026,5) > 0);
    }
    @Test void generateMonthForAll_resignedSkipped() {
        emp.setLeaveDate(LocalDate.of(2026,4,1));
        when(eRepo.findAll()).thenReturn(Collections.singletonList(emp));
        assertEquals(0, service.generateMonthForAll(2026,5));
    }
    @Test void exportCsv() {
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        when(aRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.singletonList(att));
        assertNotNull(service.exportCsv(2026,5,1L));
    }
    @Test void exportCsvAll() {
        when(eRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(aRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.singletonList(att));
        assertNotNull(service.exportCsvAll(2026,5));
    }

    private AttendanceDTO dto() {
        AttendanceDTO d = new AttendanceDTO();
        d.setEmployeeId(1L); d.setWorkDate(LocalDate.of(2026,5,1)); d.setWorkHours(8.0);
        return d;
    }
}
