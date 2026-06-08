package com.stmanagement.service;

import com.stmanagement.dto.AttendanceDTO;
import com.stmanagement.model.Attendance;
import com.stmanagement.model.Employee;
import com.stmanagement.repository.AttendanceRepository;
import com.stmanagement.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public Page<AttendanceDTO> findAll(Integer year, Integer month, Long employeeId, int page, int size) {
        Specification<Attendance> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (employeeId != null)
                predicates.add(cb.equal(root.get("employeeId"), employeeId));
            if (year != null && month != null) {
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.plusMonths(1).minusDays(1);
                predicates.add(cb.between(root.get("workDate"), start, end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return attendanceRepository.findAll(spec, PageRequest.of(page, size, Sort.by("workDate").descending()))
                .map(this::toDTO);
    }

    public AttendanceDTO findById(Long id) {
        return toDTO(attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("勤務記録が見つかりません: " + id)));
    }

    @Transactional
    public AttendanceDTO create(AttendanceDTO dto) {
        Attendance a = toEntity(dto);
        a = attendanceRepository.save(a);
        return toDTO(a);
    }

    @Transactional
    public AttendanceDTO update(Long id, AttendanceDTO dto) {
        Attendance a = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("勤務記録が見つかりません: " + id));
        a.setWorkDate(dto.getWorkDate()); a.setWorkHours(dto.getWorkHours());
        a.setOvertimeHours(dto.getOvertimeHours() != null ? dto.getOvertimeHours() : 0.0);
        if (dto.getClockIn() != null && !dto.getClockIn().isEmpty())
            a.setClockIn(java.time.LocalTime.parse(dto.getClockIn()));
        else a.setClockIn(null);
        if (dto.getClockOut() != null && !dto.getClockOut().isEmpty())
            a.setClockOut(java.time.LocalTime.parse(dto.getClockOut()));
        else a.setClockOut(null);
        // Auto-calc total hours from clock times
        if (a.getClockIn() != null && a.getClockOut() != null) {
            long mins = java.time.Duration.between(a.getClockIn(), a.getClockOut()).toMinutes();
            a.setTotalHours(Math.round(mins / 6.0) / 10.0);
        } else {
            a.setTotalHours(dto.getTotalHours());
        }
        a.setWorkType(dto.getWorkType());
        a.setStatus(dto.getStatus()); a.setRemark(dto.getRemark());
        return toDTO(attendanceRepository.save(a));
    }

    @Transactional
    public int generateMonth(Integer year, Integer month, Long employeeId) {
        if (year == null || month == null) return 0;
        int count = 0;
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            // Skip weekends
            if (d.getDayOfWeek().getValue() >= 6) continue;
            // Check if record already exists
            boolean exists = attendanceRepository
                    .findAll((Specification<Attendance>) (root, q, cb) -> cb.and(
                            cb.equal(root.get("employeeId"), employeeId),
                            cb.equal(root.get("workDate"), d)))
                    .size() > 0;
            if (!exists) {
                Attendance a = new Attendance();
                a.setEmployeeId(employeeId); a.setWorkDate(d);
                a.setWorkHours(8.0); a.setOvertimeHours(0.0);
                a.setWorkType("NORMAL"); a.setStatus("出勤");
                attendanceRepository.save(a); count++;
            }
        }
        return count;
    }

    @Transactional
    public void delete(Long id) {
        attendanceRepository.deleteById(id);
    }

    public String exportCsv(Integer year, Integer month, Long employeeId) {
        List<Attendance> list = attendanceRepository.findAll((Specification<Attendance>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (employeeId != null) predicates.add(cb.equal(root.get("employeeId"), employeeId));
            if (year != null && month != null) {
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.plusMonths(1).minusDays(1);
                predicates.add(cb.between(root.get("workDate"), start, end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        StringBuilder sb = new StringBuilder("日付,社員名,勤務時間(h),残業時間(h),ステータス,備考\n");
        for (Attendance a : list) {
            String name = employeeRepository.findById(a.getEmployeeId()).map(Employee::getName).orElse("");
            sb.append(a.getWorkDate()).append(",").append(name).append(",");
            sb.append(a.getWorkHours()).append(",").append(a.getOvertimeHours()).append(",");
            sb.append(a.getStatus()).append(",\"").append(a.getRemark() != null ? a.getRemark() : "").append("\"\n");
        }
        return sb.toString();
    }

    private AttendanceDTO toDTO(Attendance a) {
        String name = employeeRepository.findById(a.getEmployeeId()).map(Employee::getName).orElse("");
        return AttendanceDTO.builder().id(a.getId()).employeeId(a.getEmployeeId()).employeeName(name)
                .workDate(a.getWorkDate()).workHours(a.getWorkHours()).overtimeHours(a.getOvertimeHours())
                .clockIn(a.getClockIn() != null ? a.getClockIn().toString() : null)
                .clockOut(a.getClockOut() != null ? a.getClockOut().toString() : null)
                .totalHours(a.getTotalHours())
                .workType(a.getWorkType())
                .status(a.getStatus()).remark(a.getRemark()).build();
    }

    private Attendance toEntity(AttendanceDTO dto) {
        return Attendance.builder().employeeId(dto.getEmployeeId()).workDate(dto.getWorkDate())
                .workHours(dto.getWorkHours()).overtimeHours(dto.getOvertimeHours() != null ? dto.getOvertimeHours() : 0.0)
                .workType(dto.getWorkType())
                .status(dto.getStatus()).remark(dto.getRemark()).build();
    }
}
