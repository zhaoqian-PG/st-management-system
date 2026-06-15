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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceRepository attendanceRepo;
    @Mock private EmployeeRepository employeeRepo;
    @InjectMocks private AttendanceService service;

    private Attendance att;
    private Employee emp;

    @BeforeEach
    void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setName("山田 太郎"); emp.setDepartment("営業部");
        att = new Attendance(); att.setId(1L); att.setEmployeeId(1L);
        att.setWorkDate(LocalDate.of(2026, 5, 1)); att.setWorkHours(8.0);
        att.setOvertimeHours(2.0); att.setTotalHours(10.0);
        att.setWorkType("NORMAL"); att.setStatus("出勤");
        att.setClockIn(LocalTime.of(9, 0)); att.setClockOut(LocalTime.of(18, 0));
    }

    @Test
    void testFindAll() {
        Page<Attendance> page = new PageImpl<>(Collections.singletonList(att));
        when(attendanceRepo.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(page);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        Page<AttendanceDTO> result = service.findAll(null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testFindById() {
        when(attendanceRepo.findById(1L)).thenReturn(Optional.of(att));
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.findById(1L);

        assertNotNull(result);
        assertEquals(8.0, result.getWorkHours());
    }

    @Test
    void testFindById_NotFound() {
        when(attendanceRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }

    @Test
    void testCreate() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 1));
        dto.setWorkHours(8.0); dto.setWorkType("NORMAL"); dto.setStatus("出勤");

        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.create(dto);

        assertNotNull(result);
        assertEquals("出勤", result.getStatus());
        verify(attendanceRepo).save(any(Attendance.class));
    }

    @Test
    void testUpdate() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 2));
        dto.setWorkHours(7.0); dto.setOvertimeHours(1.0);
        dto.setWorkType("REMOTE"); dto.setStatus("出勤");

        when(attendanceRepo.findById(1L)).thenReturn(Optional.of(att));
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.update(1L, dto);

        assertNotNull(result);
        verify(attendanceRepo).save(any(Attendance.class));
    }

    @Test
    void testDelete() {
        doNothing().when(attendanceRepo).deleteById(1L);
        service.delete(1L);
        verify(attendanceRepo).deleteById(1L);
    }

    @Test
    void testGetMonthlySummary() {
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(att));

        Map<String, Object> result = service.getMonthlySummary(2026, 5, 1L);

        assertNotNull(result);
        assertNotNull(result.get("workHours"));
    }

    @Test
    void testGetAllEmployeeMonthlySummary() {
        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(att));

        List<Map<String, Object>> result = service.getAllEmployeeMonthlySummary(2026, 5);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

}
