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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // Check duplicate: same employee + same date
        List<Attendance> existing = attendanceRepository
                .findByEmployeeIdAndWorkDateBetween(dto.getEmployeeId(), dto.getWorkDate(), dto.getWorkDate());
        if (!existing.isEmpty()) {
            throw new RuntimeException(dto.getWorkDate() + " の勤務記録は既に存在します");
        }
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
        // Auto-calc total hours from clock times (subtract 1h lunch)
        if (a.getClockIn() != null && a.getClockOut() != null) {
            long mins = java.time.Duration.between(a.getClockIn(), a.getClockOut()).toMinutes() - 60;
            a.setTotalHours(Math.round(Math.max(0, mins) / 6.0) / 10.0);
        } else {
            a.setTotalHours(dto.getTotalHours());
        }
        a.setWorkType(dto.getWorkType());
        a.setStatus(dto.getStatus()); a.setRemark(dto.getRemark());
        return toDTO(attendanceRepository.save(a));
    }

    public List<Map<String, Object>> getAllEmployeeMonthlySummary(Integer year, Integer month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        List<Employee> allEmployees = employeeRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee emp : allEmployees) {
            // Skip resigned employees
            if (emp.getLeaveDate() != null && !emp.getLeaveDate().isAfter(end)) continue;
            List<Attendance> list = attendanceRepository
                    .findByEmployeeIdAndWorkDateBetween(emp.getId(), start, end);
            if (list.isEmpty()) continue;
            double workTotal = list.stream().mapToDouble(a -> a.getWorkHours() != null ? a.getWorkHours() : 0).sum();
            double overtimeTotal = list.stream().mapToDouble(a -> a.getOvertimeHours() != null ? a.getOvertimeHours() : 0).sum();
            double totalH = list.stream().mapToDouble(a -> a.getTotalHours() != null ? a.getTotalHours() : 0).sum();
            long workDays = list.stream().filter(a -> "出勤".equals(a.getStatus())).count();
            Map<String, Object> row = new HashMap<>();
            row.put("employeeId", emp.getId());
            row.put("employeeCode", emp.getEmployeeCode());
            row.put("employeeName", emp.getName());
            row.put("department", emp.getDepartment());
            row.put("workHours", Math.round(workTotal * 10.0) / 10.0);
            row.put("overtimeHours", Math.round(overtimeTotal * 10.0) / 10.0);
            row.put("totalHours", Math.round(totalH * 10.0) / 10.0);
            row.put("workDays", workDays);
            row.put("totalRecords", list.size());
            result.add(row);
        }
        return result;
    }

    public Map<String, Object> getMonthlySummary(Integer year, Integer month, Long employeeId) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        List<Attendance> list = attendanceRepository
                .findByEmployeeIdAndWorkDateBetween(employeeId, start, end);
        double workTotal = list.stream().mapToDouble(a -> a.getWorkHours() != null ? a.getWorkHours() : 0).sum();
        double overtimeTotal = list.stream().mapToDouble(a -> a.getOvertimeHours() != null ? a.getOvertimeHours() : 0).sum();
        double totalH = list.stream().mapToDouble(a -> a.getTotalHours() != null ? a.getTotalHours() : 0).sum();
        long workDays = list.stream().filter(a -> "出勤".equals(a.getStatus())).count();
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("workHours", Math.round(workTotal * 10.0) / 10.0);
        summary.put("overtimeHours", Math.round(overtimeTotal * 10.0) / 10.0);
        summary.put("totalHours", Math.round(totalH * 10.0) / 10.0);
        summary.put("workDays", workDays);
        summary.put("totalRecords", list.size());
        return summary;
    }

    @Transactional
    public int generateMonth(Integer year, Integer month, Long employeeId) {
        if (year == null || month == null || employeeId == null) return 0;
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        // Get existing records for this employee in this month
        List<Attendance> existing = attendanceRepository
                .findByEmployeeIdAndWorkDateBetween(employeeId, start, end);
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek().getValue() >= 6) continue; // Skip weekends
            final LocalDate date = d;
            if (existing.stream().noneMatch(a -> a.getWorkDate().equals(date))) {
                Attendance a = new Attendance();
                a.setEmployeeId(employeeId); a.setWorkDate(date);
                a.setWorkHours(8.0); a.setOvertimeHours(0.0);
                a.setWorkType("NORMAL"); a.setStatus("出勤");
                attendanceRepository.save(a); count++;
            }
        }
        return count;
    }

    @Transactional
    public int generateMonthForAll(Integer year, Integer month) {
        List<Employee> employees = employeeRepository.findAll();
        int total = 0;
        for (Employee emp : employees) {
            if (emp.getLeaveDate() != null) continue; // Skip resigned
            total += generateMonth(year, month, emp.getId());
        }
        return total;
    }

    @Transactional
    public void delete(Long id) {
        attendanceRepository.deleteById(id);
    }

    public String exportCsvAll(Integer year, Integer month) {
        StringBuilder sb = new StringBuilder();
        sb.append("社員別月次勤務集計 ").append(year).append("年").append(month).append("月\n\n");
        sb.append("社員番号,氏名,部署,勤務時間(h),残業時間(h),総労働(h),勤務日数,件数\n");
        List<Employee> employees = employeeRepository.findAll();
        double grandWork = 0, grandOvertime = 0, grandTotal = 0;
        int grandDays = 0, grandRecords = 0;
        for (Employee emp : employees) {
            if (emp.getLeaveDate() != null) continue;
            Map<String, Object> row = getMonthlySummary(year, month, emp.getId());
            long records = ((Number) row.get("totalRecords")).longValue();
            if (records == 0) continue;
            double wh = ((Number) row.get("workHours")).doubleValue();
            double oh = ((Number) row.get("overtimeHours")).doubleValue();
            double th = ((Number) row.get("totalHours")).doubleValue();
            long wd = ((Number) row.get("workDays")).longValue();
            sb.append(emp.getEmployeeCode()).append(",").append(emp.getName()).append(",");
            sb.append(emp.getDepartment() != null ? emp.getDepartment() : "").append(",");
            sb.append(wh).append(",").append(oh).append(",").append(th).append(",").append(wd).append(",");
            sb.append(records).append("\n");
            grandWork += wh; grandOvertime += oh; grandTotal += th;
            grandDays += wd; grandRecords += records;
        }
        sb.append("\n合計,,,").append(Math.round(grandWork * 10.0) / 10.0).append(",");
        sb.append(Math.round(grandOvertime * 10.0) / 10.0).append(",");
        sb.append(Math.round(grandTotal * 10.0) / 10.0).append(",");
        sb.append(grandDays).append(",").append(grandRecords).append("\n");
        return sb.toString();
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
        double totalWork = 0, totalOvertime = 0, totalHours = 0;
        StringBuilder sb = new StringBuilder("日付,社員名,勤務時間(h),残業時間(h),出勤打刻,退勤打刻,総労働(h),勤務区分,ステータス,備考\n");
        for (Attendance a : list) {
            String name = employeeRepository.findById(a.getEmployeeId()).map(Employee::getName).orElse("");
            sb.append(a.getWorkDate()).append(",").append(name).append(",");
            sb.append(a.getWorkHours()).append(",").append(a.getOvertimeHours()).append(",");
            sb.append(a.getClockIn() != null ? a.getClockIn() : "").append(",");
            sb.append(a.getClockOut() != null ? a.getClockOut() : "").append(",");
            sb.append(a.getTotalHours() != null ? a.getTotalHours() : "").append(",");
            sb.append(a.getWorkType()).append(",");
            sb.append(a.getStatus()).append(",\"").append(a.getRemark() != null ? a.getRemark() : "").append("\"\n");
            totalWork += a.getWorkHours() != null ? a.getWorkHours() : 0;
            totalOvertime += a.getOvertimeHours() != null ? a.getOvertimeHours() : 0;
            totalHours += a.getTotalHours() != null ? a.getTotalHours() : 0;
        }
        // Add summary row
        sb.append("\n月度合計,,,,");
        sb.append(String.format("%.1f,%.1f,,%.1f,,,\n", totalWork, totalOvertime, totalHours));
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
                .clockIn(dto.getClockIn() != null && !dto.getClockIn().isEmpty() ? java.time.LocalTime.parse(dto.getClockIn()) : null)
                .clockOut(dto.getClockOut() != null && !dto.getClockOut().isEmpty() ? java.time.LocalTime.parse(dto.getClockOut()) : null)
                .totalHours(dto.getTotalHours())
                .workType(dto.getWorkType())
                .status(dto.getStatus()).remark(dto.getRemark()).build();
    }
}
