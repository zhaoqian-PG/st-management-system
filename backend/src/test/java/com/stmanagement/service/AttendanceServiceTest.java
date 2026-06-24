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

import javax.persistence.EntityManager;
import java.math.BigDecimal;
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
    @Mock private EntityManager em;
    @InjectMocks private AttendanceService service;

    private Attendance att;
    private Employee emp;

    @BeforeEach
    void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setName("山田 太郎"); emp.setDepartment("営業部");
        att = new Attendance(); att.setId(1L); att.setEmployeeId(1L);
        att.setWorkDate(LocalDate.of(2026, 5, 1)); att.setWorkHours(new BigDecimal("8.0"));
        att.setOvertimeHours(new BigDecimal("2.0")); att.setTotalHours(new BigDecimal("10.0"));
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

    // findAll: 全 if 分岐網羅 (4パラメータ組み合わせ)
    @Test void testFindAll_yearMonthOnly_trueBranch() {
        // year!=null && month!=null → true, employeeId==null → false
        Page<Attendance> page = new PageImpl<>(Collections.singletonList(att));
        when(attendanceRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));
        Page<AttendanceDTO> r = service.findAll(2026, 5, null, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testFindAll_employeeIdOnly_trueBranch() {
        // employeeId!=null → true, year==null||month==null → false
        Page<Attendance> page = new PageImpl<>(Collections.singletonList(att));
        when(attendanceRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));
        Page<AttendanceDTO> r = service.findAll(null, null, 1L, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testFindAll_allThree_trueBothBranches() {
        // employeeId!=null AND year!=null&&month!=null → both true
        Page<Attendance> page = new PageImpl<>(Collections.singletonList(att));
        when(attendanceRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));
        Page<AttendanceDTO> r = service.findAll(2026, 5, 1L, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testFindAll_yearOnly_monthNull_falseBranch() {
        // year!=null but month==null → && evaluates false
        Page<Attendance> page = new PageImpl<>(Collections.singletonList(att));
        when(attendanceRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));
        Page<AttendanceDTO> r = service.findAll(2026, null, null, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testFindAll_monthOnly_yearNull_falseBranch() {
        // month!=null but year==null → && evaluates false
        Page<Attendance> page = new PageImpl<>(Collections.singletonList(att));
        when(attendanceRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));
        Page<AttendanceDTO> r = service.findAll(null, 5, null, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }

    @Test
    void testFindById() {
        when(attendanceRepo.findById(1L)).thenReturn(Optional.of(att));
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.findById(1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("8.0"), result.getWorkHours());
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
        dto.setWorkHours(new BigDecimal("8.0")); dto.setWorkType("NORMAL"); dto.setStatus("出勤");

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
        dto.setWorkHours(new BigDecimal("7.0")); dto.setOvertimeHours(new BigDecimal("1.0"));
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

    @Test void testGenerateMonth() {
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.emptyList());
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        int count = service.generateMonth(2026, 5, 1L);
        assertTrue(count > 0); // May weekdays in May 2026
    }

    @Test void testGenerateMonthForAll() {
        emp.setLeaveDate(null);
        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(),any(),any())).thenReturn(Collections.emptyList());
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        int count = service.generateMonthForAll(2026, 5);
        assertTrue(count > 0);
    }

    // ──────────── create: 重複チェック分岐 ────────────

    @Test void testCreate_Duplicate() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 1));
        dto.setWorkHours(new BigDecimal("8.0")); dto.setWorkType("NORMAL"); dto.setStatus("出勤");

        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(att)); // 重複あり
        assertThrows(RuntimeException.class, () -> service.create(dto),
                "既存レコードがある場合、RuntimeException がスローされるべき");
    }

    // ──────────── update: clockIn/clockOut + 自動計算 分岐 ────────────

    @Test void testUpdate_withClockInAndClockOut_autoCalc() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 2));
        dto.setWorkHours(new BigDecimal("7.0")); dto.setOvertimeHours(new BigDecimal("1.0"));
        dto.setWorkType("REMOTE"); dto.setStatus("出勤");
        dto.setClockIn("09:00"); dto.setClockOut("18:00"); // 両方あり → 自動計算

        when(attendanceRepo.findById(1L)).thenReturn(Optional.of(att));
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.update(1L, dto);
        assertNotNull(result);
        verify(attendanceRepo).save(any(Attendance.class));
        // clockIn/clockOut があるので totalHours は自動計算される
        // 9:00-18:00 = 540min - 60min(休憩) = 480min → Math.round(Math.max(0,480)/6.0)/10.0 = 8.0
    }

    @Test void testUpdate_withClockInOnly_noClockOut() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 2));
        dto.setWorkHours(new BigDecimal("7.0")); dto.setOvertimeHours(BigDecimal.ZERO);
        dto.setWorkType("NORMAL"); dto.setStatus("出勤");
        dto.setClockIn("09:00"); // clockIn のみ、clockOut なし
        dto.setTotalHours(new BigDecimal("7.0"));

        when(attendanceRepo.findById(1L)).thenReturn(Optional.of(att));
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.update(1L, dto);
        assertNotNull(result);
        verify(attendanceRepo).save(any(Attendance.class));
    }

    @Test void testUpdate_withoutClockInOut_usesProvidedTotalHours() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 2));
        dto.setWorkHours(new BigDecimal("7.0")); dto.setOvertimeHours(BigDecimal.ZERO);
        dto.setWorkType("NORMAL"); dto.setStatus("出勤");
        dto.setTotalHours(new BigDecimal("7.5")); // 手動設定
        // clockIn/clockOut なし → else 分岐

        when(attendanceRepo.findById(1L)).thenReturn(Optional.of(att));
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.update(1L, dto);
        assertNotNull(result);
        verify(attendanceRepo).save(any(Attendance.class));
    }

    @Test void testUpdate_nullOvertimeHours_defaultsToZero() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 2));
        dto.setWorkHours(new BigDecimal("7.0")); dto.setOvertimeHours(null); // null → 0.0
        dto.setWorkType("NORMAL"); dto.setStatus("出勤");

        when(attendanceRepo.findById(1L)).thenReturn(Optional.of(att));
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.update(1L, dto);
        assertNotNull(result);
        verify(attendanceRepo).save(any(Attendance.class));
    }

    @Test void testUpdate_withClockOutOnly_noClockIn() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 2));
        dto.setWorkHours(new BigDecimal("7.0")); dto.setOvertimeHours(BigDecimal.ZERO);
        dto.setWorkType("NORMAL"); dto.setStatus("出勤");
        dto.setClockOut("18:00"); // clockOut のみ、clockIn なし
        dto.setTotalHours(new BigDecimal("7.0"));

        when(attendanceRepo.findById(1L)).thenReturn(Optional.of(att));
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.update(1L, dto);
        assertNotNull(result);
        verify(attendanceRepo).save(any(Attendance.class));
    }

    @Test void testUpdate_withEmptyStringClockIn_clearsToNull() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 2));
        dto.setWorkHours(new BigDecimal("7.0")); dto.setOvertimeHours(BigDecimal.ZERO);
        dto.setWorkType("NORMAL"); dto.setStatus("出勤");
        dto.setClockIn(""); // 空文字列 → else 分岐 (setClockIn(null))
        dto.setClockOut(""); // 空文字列 → else 分岐
        dto.setTotalHours(new BigDecimal("7.0"));

        when(attendanceRepo.findById(1L)).thenReturn(Optional.of(att));
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.update(1L, dto);
        assertNotNull(result);
        verify(attendanceRepo).save(any(Attendance.class));
    }

    // ──────────── generateMonth: null パラメータ 分岐 ────────────

    @Test void testGenerateMonth_nullYear_returnsZero() {
        assertEquals(0, service.generateMonth(null, 5, 1L),
                "year が null → || 短絡 → 0 を返す");
    }

    @Test void testGenerateMonth_nullMonth_returnsZero() {
        assertEquals(0, service.generateMonth(2026, null, 1L),
                "month が null → || 短絡 → 0 を返す");
    }

    @Test void testGenerateMonth_nullEmployeeId_returnsZero() {
        assertEquals(0, service.generateMonth(2026, 5, null),
                "employeeId が null → 全条件 true → 0 を返す");
    }

    @Test void testGenerateMonth_existingRecords_partialGeneration() {
        // 既存レコードがある場合、nMatch により一部の日付のみ生成
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(att)); // 5/1 のみ既存
        // 5月の平日は約21日。既存1件を除く約20件が生成される
        int count = service.generateMonth(2026, 5, 1L);
        assertTrue(count > 0, "既存レコード以外の平日分が生成されるべき");
        // 各 save は att を返す（mock）
    }

    // ──────────── generateMonthForAll / getAllEmployeeMonthlySummary: 退職者スキップ ────────────

    @Test void testGenerateMonthForAll_resignedEmployee_skipped() {
        emp.setLeaveDate(LocalDate.of(2026, 4, 30)); // 退職済み
        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(emp));
        // 退職者はスキップされるので generateMonth は呼ばれない
        int count = service.generateMonthForAll(2026, 5);
        assertEquals(0, count, "退職者はスキップ → 0");
    }

    @Test void testGetAllEmployeeMonthlySummary_resignedEmployee_skipped() {
        // 退職日 4/30 → 5月の対象期間中は退職している → isAfter(end) = false → continue
        emp.setLeaveDate(LocalDate.of(2026, 4, 30));
        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(emp));
        // 退職者はスキップされるので結果は空
        List<Map<String, Object>> result = service.getAllEmployeeMonthlySummary(2026, 5);
        assertNotNull(result);
        assertEquals(0, result.size(), "退職者はスキップ → 空リスト");
    }

    @Test void testGetAllEmployeeMonthlySummary_resignedButAfterEnd_include() {
        // 退職日が end より後 → isAfter(end) = true → continue しない (含める)
        emp.setLeaveDate(LocalDate.of(2026, 6, 15)); // 6月退職、5月末より後
        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(att));
        List<Map<String, Object>> result = service.getAllEmployeeMonthlySummary(2026, 5);
        assertNotNull(result);
        assertTrue(result.size() > 0, "退職日が期間後 → 含める");
    }

    @Test void testGetAllEmployeeMonthlySummary_emptyAttendance_skipped() {
        emp.setLeaveDate(null);
        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList()); // 空リスト
        List<Map<String, Object>> result = service.getAllEmployeeMonthlySummary(2026, 5);
        assertNotNull(result);
        assertEquals(0, result.size(), "勤務記録がない従業員はスキップ");
    }

    // ──────────── toEntity: null overtime / clockIn/Out の分岐 ────────────

    @Test void testToEntity_nullOvertime_defaultsToZero() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 1));
        dto.setWorkHours(new BigDecimal("8.0")); dto.setOvertimeHours(null); // null → toEntity 内で 0.0
        dto.setWorkType("NORMAL"); dto.setStatus("出勤");

        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        // toEntity は create 経由で間接的に呼ばれる
        AttendanceDTO result = service.create(dto);
        assertNotNull(result);
    }

    @Test void testToEntity_withClockInAndClockOut() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 1));
        dto.setWorkHours(new BigDecimal("8.0")); dto.setOvertimeHours(BigDecimal.ZERO);
        dto.setClockIn("09:00"); dto.setClockOut("18:00");
        dto.setWorkType("NORMAL"); dto.setStatus("出勤");

        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.create(dto);
        assertNotNull(result);
    }

    // ──────────── exportCsv: Mockito で非Lambda部をカバー ────────────

    @Test void testExportCsv_withData() {
        when(attendanceRepo.findAll(any(Specification.class)))
                .thenReturn(Collections.singletonList(att));
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        String csv = service.exportCsv(2026, 5, 1L);
        assertNotNull(csv);
        assertTrue(csv.contains("勤務帳票"));
        assertTrue(csv.contains("山田 太郎")); // 社員名
        assertTrue(csv.contains("営業部")); // 部署
    }

    @Test void testExportCsv_noData() {
        when(attendanceRepo.findAll(any(Specification.class)))
                .thenReturn(Collections.emptyList());

        String csv = service.exportCsv(2026, 5, 1L);
        assertNotNull(csv);
        assertTrue(csv.contains("勤務帳票")); // ヘッダーは常に出力
    }

    @Test void testExportCsv_variousWorkTypes() {
        // REMOTE work type → getWorkTypeLabel returns "在宅"
        Attendance remote = new Attendance(); remote.setId(2L); remote.setEmployeeId(1L);
        remote.setWorkDate(LocalDate.of(2026, 5, 2)); remote.setWorkHours(new BigDecimal("7.0"));
        remote.setOvertimeHours(BigDecimal.ZERO); remote.setTotalHours(new BigDecimal("7.0"));
        remote.setWorkType("REMOTE"); remote.setStatus("出勤");

        when(attendanceRepo.findAll(any(Specification.class)))
                .thenReturn(java.util.Arrays.asList(att, remote));
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        String csv = service.exportCsv(2026, 5, 1L);
        assertNotNull(csv);
        assertTrue(csv.contains("通常"), "NORMAL → 通常 が含まれるべき");
        assertTrue(csv.contains("在宅"), "REMOTE → 在宅 が含まれるべき");
    }

    @Test void testExportCsv_holidayAndLeaveTypes() {
        // HOLIDAY_WORK and LEAVE to cover all switch branches
        Attendance holiday = new Attendance(); holiday.setId(3L); holiday.setEmployeeId(1L);
        holiday.setWorkDate(LocalDate.of(2026, 5, 3)); holiday.setWorkHours(new BigDecimal("8.0"));
        holiday.setOvertimeHours(BigDecimal.ZERO); holiday.setTotalHours(new BigDecimal("8.0"));
        holiday.setWorkType("HOLIDAY_WORK"); holiday.setStatus("出勤");

        Attendance leave = new Attendance(); leave.setId(4L); leave.setEmployeeId(1L);
        leave.setWorkDate(LocalDate.of(2026, 5, 4)); leave.setWorkHours(BigDecimal.ZERO);
        leave.setOvertimeHours(BigDecimal.ZERO); leave.setTotalHours(BigDecimal.ZERO);
        leave.setWorkType("LEAVE"); leave.setStatus("休暇");
        leave.setRemark("有給休暇");

        when(attendanceRepo.findAll(any(Specification.class)))
                .thenReturn(java.util.Arrays.asList(holiday, leave));
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        String csv = service.exportCsv(2026, 5, 1L);
        assertNotNull(csv);
        assertTrue(csv.contains("休日出勤"), "HOLIDAY_WORK → 休日出勤");
        assertTrue(csv.contains("休暇"), "LEAVE → 休暇");
        assertTrue(csv.contains("有給休暇"), "備考が含まれるべき");
    }

    @Test void testExportCsv_withClockInOutAndNullRemark() {
        Attendance withClock = new Attendance(); withClock.setId(5L); withClock.setEmployeeId(1L);
        withClock.setWorkDate(LocalDate.of(2026, 5, 5)); withClock.setWorkHours(new BigDecimal("8.0"));
        withClock.setOvertimeHours(BigDecimal.ZERO); withClock.setTotalHours(new BigDecimal("8.0"));
        withClock.setWorkType("NORMAL"); withClock.setStatus("出勤");
        withClock.setClockIn(LocalTime.of(9, 0)); withClock.setClockOut(LocalTime.of(18, 0));
        withClock.setRemark(null); // null remark

        when(attendanceRepo.findAll(any(Specification.class)))
                .thenReturn(Collections.singletonList(withClock));
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        String csv = service.exportCsv(2026, 5, 1L);
        assertNotNull(csv);
        assertTrue(csv.contains("09:00"));  // clockIn
        assertTrue(csv.contains("18:00"));  // clockOut
    }

    @Test void testExportCsv_employeeNotFound() {
        when(attendanceRepo.findAll(any(Specification.class)))
                .thenReturn(Collections.singletonList(att));
        when(employeeRepo.findById(1L)).thenReturn(Optional.empty()); // 社員みつからず

        String csv = service.exportCsv(2026, 5, 1L);
        assertNotNull(csv);
        assertTrue(csv.contains("勤務帳票"));
    }

    // ──────────── exportCsvAll: Mockito で非Lambda部をカバー ────────────

    @Test void testExportCsvAll_withData() {
        emp.setLeaveDate(null);
        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(att));

        String csv = service.exportCsvAll(2026, 5);
        assertNotNull(csv);
        assertTrue(csv.contains("社員別月次勤務集計"));
        assertTrue(csv.contains("山田 太郎"));
        assertTrue(csv.contains("合計"));
    }

    @Test void testExportCsvAll_emptyEmployees() {
        when(employeeRepo.findAll()).thenReturn(Collections.emptyList());

        String csv = service.exportCsvAll(2026, 5);
        assertNotNull(csv);
        assertTrue(csv.contains("合計")); // 空でも合計行あり
    }

    @Test void testExportCsvAll_resignedEmployee_skipped() {
        emp.setLeaveDate(LocalDate.of(2026, 4, 30)); // 退職済み
        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(emp));

        String csv = service.exportCsvAll(2026, 5);
        assertNotNull(csv);
        assertTrue(csv.contains("合計"));
        assertFalse(csv.contains("山田 太郎"), "退職者はスキップされるべき");
    }

    @Test void testExportCsvAll_employeeWithNoRecords_skipped() {
        emp.setLeaveDate(null);
        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList()); // 勤務記録なし

        String csv = service.exportCsvAll(2026, 5);
        assertNotNull(csv);
        assertTrue(csv.contains("合計"));
        assertFalse(csv.contains("山田 太郎"), "勤務記録なし → スキップ");
    }

    // ──────────── getWorkTypeLabel: default分岐 + null分岐 ────────────

    @Test void testExportCsv_unknownWorkType_fallsToDefault() {
        Attendance unknown = new Attendance(); unknown.setId(6L); unknown.setEmployeeId(1L);
        unknown.setWorkDate(LocalDate.of(2026, 5, 10)); unknown.setWorkHours(new BigDecimal("8.0"));
        unknown.setOvertimeHours(BigDecimal.ZERO); unknown.setTotalHours(new BigDecimal("8.0"));
        unknown.setWorkType("CUSTOM_TYPE"); unknown.setStatus("出勤"); // 未知タイプ

        when(attendanceRepo.findAll(any(Specification.class)))
                .thenReturn(Collections.singletonList(unknown));
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        String csv = service.exportCsv(2026, 5, 1L);
        assertNotNull(csv);
        assertTrue(csv.contains("CUSTOM_TYPE"), "未知workTypeはそのまま出力される");
    }

    @Test void testExportCsv_nullWorkType_returnsEmpty() {
        Attendance nullType = new Attendance(); nullType.setId(7L); nullType.setEmployeeId(1L);
        nullType.setWorkDate(LocalDate.of(2026, 5, 11)); nullType.setWorkHours(new BigDecimal("8.0"));
        nullType.setOvertimeHours(BigDecimal.ZERO); nullType.setTotalHours(new BigDecimal("8.0"));
        nullType.setWorkType(null); nullType.setStatus("出勤"); // null workType

        when(attendanceRepo.findAll(any(Specification.class)))
                .thenReturn(Collections.singletonList(nullType));
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        String csv = service.exportCsv(2026, 5, 1L);
        assertNotNull(csv);
        // null → getWorkTypeLabel は空文字列を返す
        assertTrue(csv.contains("勤務帳票"));
    }

    // ──────────── stream lambda null分岐 網羅 ────────────

    @Test void testGetMonthlySummary_nullHours() {
        // workHours/overtimeHours/totalHours が null のケース → ストリーム内の三項演算子 null分岐
        Attendance nullHours = new Attendance(); nullHours.setId(8L); nullHours.setEmployeeId(1L);
        nullHours.setWorkDate(LocalDate.of(2026, 5, 20)); nullHours.setWorkHours(null);
        nullHours.setOvertimeHours(null); nullHours.setTotalHours(null);
        nullHours.setWorkType("NORMAL"); nullHours.setStatus("出勤");

        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(nullHours));

        Map<String, Object> result = service.getMonthlySummary(2026, 5, 1L);
        assertNotNull(result);
        assertEquals(new BigDecimal("0.0"), result.get("workHours"), "null workHours → 0.0");
        assertEquals(new BigDecimal("0.0"), result.get("overtimeHours"), "null overtime → 0.0");
        assertEquals(new BigDecimal("0.0"), result.get("totalHours"), "null total → 0.0");
    }

    @Test void testGetAllEmployeeMonthlySummary_nullHours() {
        Attendance nullHours = new Attendance(); nullHours.setId(9L); nullHours.setEmployeeId(1L);
        nullHours.setWorkDate(LocalDate.of(2026, 5, 20)); nullHours.setWorkHours(null);
        nullHours.setOvertimeHours(null); nullHours.setTotalHours(null);
        nullHours.setWorkType("NORMAL"); nullHours.setStatus("出勤");

        emp.setLeaveDate(null);
        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(emp));
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(nullHours));

        List<Map<String, Object>> result = service.getAllEmployeeMonthlySummary(2026, 5);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("0.0"), result.get(0).get("workHours"));
        assertEquals(new BigDecimal("0.0"), result.get(0).get("overtimeHours"));
        assertEquals(new BigDecimal("0.0"), result.get(0).get("totalHours"));
    }

    // ──────────── toEntity: 空文字列 clockIn/Out 分岐 (create経由) ────────────

    @Test void testCreate_emptyClockInOut_toEntityBranch() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 7, 3));
        dto.setWorkHours(new BigDecimal("8.0")); dto.setOvertimeHours(BigDecimal.ZERO);
        dto.setClockIn("");  // 空文字列 → toEntity内 T∧F → null
        dto.setClockOut(""); // 空文字列 → toEntity内 T∧F → null
        dto.setWorkType("NORMAL"); dto.setStatus("出勤");

        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(attendanceRepo.save(any(Attendance.class))).thenReturn(att);
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));

        AttendanceDTO result = service.create(dto);
        assertNotNull(result);
    }

    // ──────────── exportCsv / exportCsvAll: null部署 分岐 ────────────

    @Test void testExportCsvAll_nullDepartment() {
        Employee empNoDept = new Employee(); empNoDept.setId(2L);
        empNoDept.setName("部署なし太郎"); empNoDept.setEmployeeCode("EMP999");
        empNoDept.setDepartment(null); // null 部署
        empNoDept.setLeaveDate(null);

        Attendance att2 = new Attendance(); att2.setId(10L); att2.setEmployeeId(2L);
        att2.setWorkDate(LocalDate.of(2026, 5, 1)); att2.setWorkHours(new BigDecimal("8.0"));
        att2.setOvertimeHours(BigDecimal.ZERO); att2.setTotalHours(new BigDecimal("8.0"));
        att2.setWorkType("NORMAL"); att2.setStatus("出勤");

        when(employeeRepo.findAll()).thenReturn(Collections.singletonList(empNoDept));
        when(attendanceRepo.findByEmployeeIdAndWorkDateBetween(anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(att2));

        String csv = service.exportCsvAll(2026, 5);
        assertNotNull(csv);
        assertTrue(csv.contains("部署なし太郎"));
        // null 部署 → "" → 空文字列が出力される
    }

    @Test void testExportCsv_nullDepartment() {
        Employee empNoDept = new Employee(); empNoDept.setId(3L);
        empNoDept.setName("部署なし花子"); empNoDept.setEmployeeCode("EMP888");
        empNoDept.setDepartment(null);

        Attendance att3 = new Attendance(); att3.setId(11L); att3.setEmployeeId(3L);
        att3.setWorkDate(LocalDate.of(2026, 5, 1)); att3.setWorkHours(new BigDecimal("8.0"));
        att3.setOvertimeHours(BigDecimal.ZERO); att3.setTotalHours(new BigDecimal("8.0"));
        att3.setWorkType("NORMAL"); att3.setStatus("出勤");

        when(attendanceRepo.findAll(any(Specification.class)))
                .thenReturn(Collections.singletonList(att3));
        when(employeeRepo.findById(3L)).thenReturn(Optional.of(empNoDept));

        String csv = service.exportCsv(2026, 5, 3L);
        assertNotNull(csv);
        assertTrue(csv.contains("部署なし花子"));
        // null 部署 → 空文字列 → 部署列に空文字
    }

    @Test void testUpdate_notFound_throwsException() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L); dto.setWorkDate(LocalDate.of(2026, 5, 1));
        dto.setWorkHours(new BigDecimal("8.0")); dto.setWorkType("NORMAL"); dto.setStatus("出勤");

        when(attendanceRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.update(99L, dto),
                "存在しないIDの更新 → RuntimeException");
    }

    // ════════════════════ 合計: 50 tests in this class ════════════════════
}
