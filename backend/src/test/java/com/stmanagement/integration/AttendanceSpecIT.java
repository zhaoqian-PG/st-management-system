package com.stmanagement.integration;

import com.stmanagement.dto.AttendanceDTO;
import com.stmanagement.model.Attendance;
import com.stmanagement.model.Employee;
import com.stmanagement.repository.AttendanceRepository;
import com.stmanagement.repository.EmployeeRepository;
import com.stmanagement.service.AttendanceService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>AttendanceService Specification 分岐網羅 統合テスト</h1>
 *
 * <p>本テストの目的：
 * Mockito 単体テストでは {@code attendanceRepository.findAll(Specification, Pageable)}
 * がモックで差し替えられ、Specification 内部の Lambda ({@code if (employeeId != null)}
 * や {@code if (year != null && month != null)}) が一切実行されない。
 * JaCoCo はこれらの分岐を「未到達」と報告する（赤色表示）。</p>
 *
 * <p>本クラスでは {@code @SpringBootTest} + H2 インメモリ DB により、
 * <b>本物の JPA プロバイダ (Hibernate)</b> に Specification を解析・実行させる。
 * 事前に様々な employeeId / year / month の Attendance データを登録し、
 * 異なるパラメータ組み合わせで {@code findAll()} / {@code exportCsv()} を呼び出すことで、
 * Lambda 内の全 if 分岐を強制的に踏破する。</p>
 *
 * <h2>カバレッジ対象メソッド</h2>
 * <ul>
 *   <li>{@code AttendanceService.findAll(Integer year, Integer month, Long employeeId, ...)}</li>
 *   <li>{@code AttendanceService.exportCsv(Integer year, Integer month, Long employeeId)}</li>
 * </ul>
 *
 * <h2>テストデータ設計 (6件)</h2>
 * <pre>
 * | empId | workDate   | 所属     |
 * |-------|-----------|---------|
 * |   1   | 2026-05-01| 5月     |
 * |   1   | 2026-05-15| 5月     |
 * |   1   | 2026-06-01| 6月     |
 * |   2   | 2026-05-01| 5月     |
 * |   2   | 2026-06-15| 6月     |
 * |   3   | 2026-03-01| 3月     |
 * </pre>
 *
 * <h2>Specification if 分岐 真理値表 (6組み合わせ)</h2>
 * <pre>
 * # | empId!=null | year!=null | month!=null | &&結果  | 期待件数
 * --|-------------|-----------|-------------|--------|--------
 * ①| T           | T         | T           | T      | 2 (emp1, 5月)
 * ②| T           | F         | - (未評価)   | F      | 3 (emp1 全件)
 * ③| T           | T         | F           | F      | 3 (emp1 全件)
 * ④| F           | T         | T           | T      | 3 (全員, 5月)
 * ⑤| F           | F         | - (未評価)   | F      | 6 (全件)
 * ⑥| F           | F         | F           | F      | 6 (全件)
 * </pre>
 *
 * @author ST Management Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AttendanceSpecIT {

    @Autowired
    private AttendanceService service;

    @Autowired
    private AttendanceRepository attendanceRepo;

    @Autowired
    private EmployeeRepository employeeRepo;

    @PersistenceContext
    private EntityManager em;

    /** 全6件のテストデータ（検証用に保持） */
    private final List<Attendance> allRecords = new ArrayList<>();

    /** 動的に解決された社員ID（H2 IDENTITY カウンタ非リセット対応） */
    private Long emp1Id, emp2Id, emp3Id;

    /** テストメソッド間で異なるIDに対応するためのマップ: emp名 → 現在のID */
    private java.util.Map<String, Long> empIds;

    // ───────────────────── テストデータ準備 ─────────────────────

    @BeforeEach
    void setUp() {
        // Employee を native SQL で投入
        // （employee_code は insertable=false のため JPA persist 不可 → native INSERT で回避）
        em.createNativeQuery("INSERT INTO employee (name, employee_code, department, status, create_time, update_time) " +
                "VALUES ('山田 太郎', 'EMP001', '営業部', '在職', NOW(), NOW())").executeUpdate();
        em.createNativeQuery("INSERT INTO employee (name, employee_code, department, status, create_time, update_time) " +
                "VALUES ('佐藤 花子', 'EMP002', '開発部', '在職', NOW(), NOW())").executeUpdate();
        em.createNativeQuery("INSERT INTO employee (name, employee_code, department, status, create_time, update_time) " +
                "VALUES ('鈴木 次郎', 'EMP003', '管理部', '在職', NOW(), NOW())").executeUpdate();
        em.flush();
        em.clear(); // Hibernate 1次キャッシュをクリア → 後続の findById が DB から再読込

        // H2 IDENTITY カウンタは @Transactional ロールバック後もリセットされないため、
        // 実際に割り当てられたIDを動的に取得
        empIds = new java.util.HashMap<>();
        for (Object[] row : (java.util.List<Object[]>) em.createNativeQuery(
                "SELECT name, id FROM employee ORDER BY id").getResultList()) {
            empIds.put((String) row[0], ((Number) row[1]).longValue());
        }
        emp1Id = empIds.get("山田 太郎");
        emp2Id = empIds.get("佐藤 花子");
        emp3Id = empIds.get("鈴木 次郎");

        // Attendance 6件 投入 (年・月・社員 を意図的に分散)
        allRecords.clear();
        allRecords.add(saveAttendance(emp1Id, LocalDate.of(2026, 5, 1), 8.0));
        allRecords.add(saveAttendance(emp1Id, LocalDate.of(2026, 5, 15), 8.0));
        allRecords.add(saveAttendance(emp1Id, LocalDate.of(2026, 6, 1), 7.0));
        allRecords.add(saveAttendance(emp2Id, LocalDate.of(2026, 5, 1), 8.0));
        allRecords.add(saveAttendance(emp2Id, LocalDate.of(2026, 6, 15), 8.0));
        allRecords.add(saveAttendance(emp3Id, LocalDate.of(2026, 3, 1), 8.0));
        em.flush();
    }

    private Attendance saveAttendance(Long empId, LocalDate date, Double hours) {
        Attendance a = Attendance.builder()
                .employeeId(empId)
                .workDate(date)
                .workHours(hours)
                .overtimeHours(0.0)
                .workType("NORMAL")
                .status("出勤")
                .build();
        return attendanceRepo.save(a);
    }

    // ───────────────── findAll: Specification 分岐網羅 ─────────────────

    /**
     * ① empId=T, year=T, month=T → 両方の if ブロックが実行される
     * SQL: WHERE employee_id=1 AND work_date BETWEEN '2026-05-01' AND '2026-05-31'
     */
    @Test
    @DisplayName("findAll ① empIdあり＋年月あり → 両分岐 true")
    void specFindAll_empIdPresent_yearMonthPresent_bothBranchesTrue() {
        Page<AttendanceDTO> result = service.findAll(2026, 5, emp1Id, 0, 10);

        assertNotNull(result, "結果ページはnullであってはならない");
        assertEquals(2, result.getTotalElements(),
                "社員1の5月のレコードは2件であるべき");
        // 全結果が employeeId=1 かつ workDate が5月内であることを検証
        for (AttendanceDTO dto : result.getContent()) {
            assertEquals(emp1Id, dto.getEmployeeId(), "社員IDは1であるべき");
            LocalDate d = dto.getWorkDate();
            assertTrue(d.getYear() == 2026 && d.getMonthValue() == 5,
                    "日付は2026年5月であるべき: " + d);
        }
    }

    /**
     * ② empId=T, year=null, month=任意
     *    → employeeId 分岐 true, year!=null が false → && 短絡評価 (month 未評価)
     * SQL: WHERE employee_id=1 (日付絞り込みなし)
     */
    @Test
    @DisplayName("findAll ② empIdあり＋year=null → &&短絡 (month未評価)")
    void specFindAll_empIdPresent_yearNull_andShortCircuit() {
        Page<AttendanceDTO> result = service.findAll(null, 5, emp1Id, 0, 10);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements(),
                "社員1の全レコードは3件 (5月2件＋6月1件)");
        for (AttendanceDTO dto : result.getContent()) {
            assertEquals(emp1Id, dto.getEmployeeId());
        }
    }

    /**
     * ③ empId=T, year=T, month=null
     *    → employeeId 分岐 true, year!=null true, month!=null false → && 結果 false
     * SQL: WHERE employee_id=1 (日付絞り込みなし)
     */
    @Test
    @DisplayName("findAll ③ empIdあり＋yearあり＋month=null → &&false")
    void specFindAll_empIdPresent_yearPresent_monthNull_andFalse() {
        Page<AttendanceDTO> result = service.findAll(2026, null, emp1Id, 0, 10);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements(),
                "month=null のため日付絞り込みは適用されず、社員1の全3件が返る");
        for (AttendanceDTO dto : result.getContent()) {
            assertEquals(emp1Id, dto.getEmployeeId());
        }
    }

    /**
     * ④ empId=null, year=T, month=T
     *    → employeeId 分岐 false, year!=null && month!=null true → 日付分岐のみ実行
     * SQL: WHERE work_date BETWEEN '2026-05-01' AND '2026-05-31'
     */
    @Test
    @DisplayName("findAll ④ empId=null＋年月あり → 日付分岐のみ true")
    void specFindAll_empIdNull_yearMonthPresent_dateBranchTrue() {
        Page<AttendanceDTO> result = service.findAll(2026, 5, null, 0, 10);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements(),
                "5月のレコードは全社員合計3件 (emp1×2 + emp2×1)");
        for (AttendanceDTO dto : result.getContent()) {
            LocalDate d = dto.getWorkDate();
            assertEquals(2026, d.getYear());
            assertEquals(5, d.getMonthValue());
        }
        // 社員1(2件) と 社員2(1件) の両方が含まれていることを確認
        Set<Long> empIds = result.getContent().stream()
                .map(AttendanceDTO::getEmployeeId).collect(Collectors.toSet());
        assertTrue(empIds.contains(emp1Id) && empIds.contains(emp2Id),
                "社員1と社員2の両方のレコードが含まれているべき");
    }

    /**
     * ⑤ empId=null, year=null, month=任意
     *    → employeeId 分岐 false, year!=null false → && 短絡評価
     * SQL: 絞り込みなし (全件)
     */
    @Test
    @DisplayName("findAll ⑤ empId=null＋year=null → &&短絡 (全件)")
    void specFindAll_empIdNull_yearNull_andShortCircuit() {
        Page<AttendanceDTO> result = service.findAll(null, 5, null, 0, 10);

        assertNotNull(result);
        assertEquals(6, result.getTotalElements(),
                "year=null で日付絞り込みなし → 全6件が返る");
    }

    /**
     * ⑥ empId=null, year=null, month=null
     *    → 両分岐とも false (最も単純な全件検索)
     * SQL: 絞り込みなし (全件)
     */
    @Test
    @DisplayName("findAll ⑥ 全パラメータ null → 両分岐 false")
    void specFindAll_allNull_bothBranchesFalse() {
        Page<AttendanceDTO> result = service.findAll(null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(6, result.getTotalElements(), "全件検索は6件返るべき");
        // 全3社員のデータが含まれていることを確認
        Set<Long> empIds = result.getContent().stream()
                .map(AttendanceDTO::getEmployeeId).collect(Collectors.toSet());
        assertEquals(3, empIds.size(), "3人の社員データが含まれているべき");
    }

    // ───────────────── findAll: ソート順検証 ─────────────────

    /**
     * findAll は workDate DESC でソートされることを確認
     */
    @Test
    @DisplayName("findAll ソート順: workDate DESC であること")
    void specFindAll_sortOrderDescending() {
        Page<AttendanceDTO> result = service.findAll(2026, 5, emp1Id, 0, 10);

        assertEquals(2, result.getTotalElements());
        // DESC: 5月15日 → 5月1日 の順
        assertEquals(LocalDate.of(2026, 5, 15),
                result.getContent().get(0).getWorkDate());
        assertEquals(LocalDate.of(2026, 5, 1),
                result.getContent().get(1).getWorkDate());
    }

    // ───────────────── findAll: ページネーション検証 ─────────────────

    /**
     * ページネーションが Specification と組み合わさっても正しく機能すること
     */
    @Test
    @DisplayName("findAll ページネーション: 全6件 → size=3 で2ページ")
    void specFindAll_pagination() {
        Page<AttendanceDTO> page1 = service.findAll(null, null, null, 0, 3);
        assertEquals(6, page1.getTotalElements());
        assertEquals(2, page1.getTotalPages());
        assertEquals(3, page1.getContent().size(), "1ページ目は3件");

        Page<AttendanceDTO> page2 = service.findAll(null, null, null, 1, 3);
        assertEquals(3, page2.getContent().size(), "2ページ目も3件");

        // ページ間で重複がないことを確認
        Set<Long> ids1 = page1.getContent().stream()
                .map(AttendanceDTO::getId).collect(Collectors.toSet());
        Set<Long> ids2 = page2.getContent().stream()
                .map(AttendanceDTO::getId).collect(Collectors.toSet());
        ids1.retainAll(ids2);
        assertTrue(ids1.isEmpty(), "ページ間でレコードの重複があってはならない");
    }

    // ───────────────── exportCsv: Specification 分岐網羅 ─────────────────

    /**
     * exportCsv も findAll と同一の Specification パターンを持つ。
     * 全く同じ if 分岐が存在するため、同様に全パラメータ組み合わせをカバーする。
     */

    @Test
    @DisplayName("exportCsv ① empIdあり＋年月あり → 両分岐 true")
    void specExportCsv_empIdPresent_yearMonthPresent_bothBranchesTrue() {
        String csv = service.exportCsv(2026, 5, emp1Id);

        assertNotNull(csv);
        assertTrue(csv.contains("勤務帳票"), "CSVヘッダーが含まれているべき");
        // 5月の2件の日付が含まれている → Specification の年月絞り込みが実行された証拠
        assertTrue(csv.contains("2026-05-01"), "5月1日のレコードが含まれているべき");
        assertTrue(csv.contains("2026-05-15"), "5月15日のレコードが含まれているべき");
        // 6月のレコードは含まれていない（年月フィルタが効いている）
        assertFalse(csv.contains("2026-06-01"), "6月のレコードは含まれるべきではない");
    }

    @Test
    @DisplayName("exportCsv ② empIdあり＋year=null → &&短絡")
    void specExportCsv_empIdPresent_yearNull_andShortCircuit() {
        String csv = service.exportCsv(null, 5, emp1Id);

        assertNotNull(csv);
        assertTrue(csv.contains("勤務帳票"));
        // year=null → 日付絞り込みなし → 社員1の全3件（5月＋6月）
        assertTrue(csv.contains("2026-05-01"));
        assertTrue(csv.contains("2026-06-01"));
    }

    @Test
    @DisplayName("exportCsv ③ empIdあり＋yearあり＋month=null → &&false")
    void specExportCsv_empIdPresent_yearPresent_monthNull_andFalse() {
        String csv = service.exportCsv(2026, null, emp1Id);

        assertNotNull(csv);
        assertTrue(csv.contains("勤務帳票"));
        // month=null → 日付絞り込みなし、社員1の全3件（5月＋6月）
        assertTrue(csv.contains("2026-05-01"));
        assertTrue(csv.contains("2026-06-01"));
    }

    @Test
    @DisplayName("exportCsv ④ empId=null＋年月あり → 日付分岐のみ true")
    void specExportCsv_empIdNull_yearMonthPresent_dateBranchTrue() {
        String csv = service.exportCsv(2026, 5, null);

        assertNotNull(csv);
        assertTrue(csv.contains("勤務帳票"));
        // 全社員の5月レコード (emp1×2 + emp2×1 = 3件)
        assertTrue(csv.contains("2026-05-01"));
        assertTrue(csv.contains("2026-05-15"));
        assertFalse(csv.contains("2026-06"), "6月のレコードは含まれるべきではない");
    }

    @Test
    @DisplayName("exportCsv ⑤ empId=null＋year=null → &&短絡 (全件)")
    void specExportCsv_empIdNull_yearNull_andShortCircuit() {
        String csv = service.exportCsv(null, 5, null);

        assertNotNull(csv);
        assertTrue(csv.contains("勤務帳票"));
        // year=null → &&短絡 → 絞り込みなし → 全6件（3月も含まれる）
        assertTrue(csv.contains("2026-03-01"), "year=null → 全件、3月も含まれるべき");
    }

    @Test
    @DisplayName("exportCsv ⑥ 全パラメータ null → 両分岐 false")
    void specExportCsv_allNull_bothBranchesFalse() {
        String csv = service.exportCsv(null, null, null);

        assertNotNull(csv);
        assertTrue(csv.contains("勤務帳票"));
        // 全6件 → 3月・6月のレコードも含まれる
        assertTrue(csv.contains("2026-03-01"));
        assertTrue(csv.contains("2026-06-01"));
        assertTrue(csv.contains("2026-06-15"));
    }

    // ───────────────── 実務的な複合検証 ─────────────────

    /**
     * クロスチェック: findById と findAll の結果整合性
     */
    @Test
    @DisplayName("クロスチェック: findAll(emp1,5月) の結果が findById と一致する")
    void crossCheck_findAllMatchesFindById() {
        Page<AttendanceDTO> page = service.findAll(2026, 5, emp1Id, 0, 10);
        assertEquals(2, page.getTotalElements());

        for (AttendanceDTO dto : page.getContent()) {
            AttendanceDTO byId = service.findById(dto.getId());
            assertEquals(dto.getWorkDate(), byId.getWorkDate());
            assertEquals(dto.getWorkHours(), byId.getWorkHours());
            assertEquals(dto.getEmployeeId(), byId.getEmployeeId());
        }
    }

    /**
     * 境界値: 該当データが存在しないクエリ
     */
    @Test
    @DisplayName("境界値: 存在しない社員ID → 空結果")
    void edgeCase_nonExistentEmployee_returnsEmpty() {
        Page<AttendanceDTO> result = service.findAll(2026, 5, 999L, 0, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements(),
                "存在しない社員ID 999 → 0件");
    }

    /**
     * 境界値: 存在しない年月
     */
    @Test
    @DisplayName("境界値: データのない年月 → 空結果")
    void edgeCase_noDataForYearMonth_returnsEmpty() {
        Page<AttendanceDTO> result = service.findAll(2025, 1, null, 0, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements(),
                "2025年1月のデータは存在しない → 0件");
    }

    /**
     * 境界値: 存在しない社員＋存在しない年月の組み合わせ
     */
    @Test
    @DisplayName("境界値: 存在しない社員＋年月 → 空結果（両分岐実行）")
    void edgeCase_nonExistentEmployeeAndYearMonth_bothBranchesExecuted() {
        Page<AttendanceDTO> result = service.findAll(2025, 1, 999L, 0, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements(),
                "両方のフィルタが適用された上で0件");
        // このケースでは両方の if ブロックが実行される（条件は true）
        // 結果が0件でも Specification の分岐は踏まれている
    }

    // ════════════════════ exportCsvAll ════════════════════

    @Test
    @DisplayName("exportCsvAll: 基本動作")
    void exportCsvAll_basic() {
        String csv = service.exportCsvAll(2026, 5);
        assertNotNull(csv);
        assertTrue(csv.contains("社員別月次勤務集計"), "ヘッダーが含まれているべき");
        assertTrue(csv.contains("EMP001"), "山田太郎のEMP001が含まれているべき");
    }

    @Test
    @DisplayName("exportCsvAll: 存在しない年月 → 空集計")
    void exportCsvAll_emptyYearMonth() {
        String csv = service.exportCsvAll(2025, 1);
        assertNotNull(csv);
        assertTrue(csv.contains("合計"), "空でも合計行は出力されるべき");
    }

    // ════════════════════ getMonthlySummary ════════════════════

    @Test
    @DisplayName("getMonthlySummary: 基本集計")
    void monthlySummary_basic() {
        Map<String, Object> summary = service.getMonthlySummary(2026, 5, emp1Id);
        assertNotNull(summary);
        assertEquals(16.0, summary.get("workHours"),
                "emp1の5月の所定勤務は 8+8=16.0");
        assertEquals(2L, summary.get("totalRecords"),
                "emp1の5月のレコードは2件");
    }

    @Test
    @DisplayName("getMonthlySummary: データなし")
    void monthlySummary_noData() {
        Map<String, Object> summary = service.getMonthlySummary(2025, 1, emp1Id);
        assertNotNull(summary);
        assertEquals(0.0, summary.get("workHours"));
        assertEquals(0L, summary.get("totalRecords"));
    }

    // ════════════════════ getAllEmployeeMonthlySummary ════════════════════

    @Test
    @DisplayName("getAllEmployeeMonthlySummary: 基本集計")
    void allEmployeeMonthlySummary_basic() {
        List<Map<String, Object>> result = service.getAllEmployeeMonthlySummary(2026, 5);
        assertNotNull(result);
        assertTrue(result.size() >= 2, "5月はemp1とemp2の2名分");
    }

    @Test
    @DisplayName("getAllEmployeeMonthlySummary: データなし → 空リスト")
    void allEmployeeMonthlySummary_empty() {
        List<Map<String, Object>> result = service.getAllEmployeeMonthlySummary(2025, 1);
        assertNotNull(result);
        assertEquals(0, result.size(), "2025年1月はデータなし → 空リスト");
    }

    // ════════════════════ generateMonth ════════════════════

    @Test
    @DisplayName("generateMonth: 基本生成")
    void generateMonth_basic() {
        int count = service.generateMonth(2026, 5, emp1Id);
        assertTrue(count > 0, "5月の平日に対して勤務レコードが生成されるべき");
        // emp1 の5月には既に2レコード存在 → 残りの平日分が生成
    }

    @Test
    @DisplayName("generateMonth: null year → 0")
    void generateMonth_nullYear_returnsZero() {
        assertEquals(0, service.generateMonth(null, 5, emp1Id));
    }

    @Test
    @DisplayName("generateMonth: null month → 0")
    void generateMonth_nullMonth_returnsZero() {
        assertEquals(0, service.generateMonth(2026, null, emp1Id));
    }

    @Test
    @DisplayName("generateMonth: null employeeId → 0")
    void generateMonth_nullEmployeeId_returnsZero() {
        assertEquals(0, service.generateMonth(2026, 5, null));
    }

    // ════════════════════ generateMonthForAll ════════════════════

    @Test
    @DisplayName("generateMonthForAll: 全社員生成")
    void generateMonthForAll_basic() {
        int count = service.generateMonthForAll(2026, 5);
        assertTrue(count > 0, "全社員分の平日レコードが生成されるべき");
    }

    // ════════════════════ create ════════════════════

    @Test
    @DisplayName("create: 新規レコード作成")
    void create_newRecord() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(emp1Id);
        dto.setWorkDate(LocalDate.of(2026, 7, 1));
        dto.setWorkHours(8.0);
        dto.setOvertimeHours(0.0);
        dto.setWorkType("NORMAL");
        dto.setStatus("出勤");

        AttendanceDTO result = service.create(dto);
        assertNotNull(result);
        assertNotNull(result.getId(), "ID が採番されているべき");
        assertEquals(emp1Id, result.getEmployeeId());
        assertEquals(LocalDate.of(2026, 7, 1), result.getWorkDate());
    }

    @Test
    @DisplayName("create: 重複 → RuntimeException")
    void create_duplicate_throwsException() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(emp1Id);
        dto.setWorkDate(LocalDate.of(2026, 5, 1)); // 既存レコードと重複
        dto.setWorkHours(8.0);
        dto.setWorkType("NORMAL");
        dto.setStatus("出勤");

        assertThrows(RuntimeException.class, () -> service.create(dto),
                "同日・同社員のレコードが既に存在するため例外");
    }

    @Test
    @DisplayName("create: clockIn/clockOut あり")
    void create_withClockInAndClockOut() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(emp1Id);
        dto.setWorkDate(LocalDate.of(2026, 7, 2));
        dto.setWorkHours(8.0);
        dto.setOvertimeHours(0.0);
        dto.setClockIn("09:00");
        dto.setClockOut("18:00");
        dto.setWorkType("NORMAL");
        dto.setStatus("出勤");

        AttendanceDTO result = service.create(dto);
        assertNotNull(result);
        assertNotNull(result.getId());
    }

    // ════════════════════ update ════════════════════

    @Test
    @DisplayName("update: clockIn/clockOut → 自動計算")
    void update_withClockInOut_autoCalc() {
        // emp1 の既存レコードを更新
        Long existingId = allRecords.get(0).getId();

        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(emp1Id);
        dto.setWorkDate(LocalDate.of(2026, 5, 1));
        dto.setWorkHours(8.0);
        dto.setOvertimeHours(0.0);
        dto.setClockIn("09:00");
        dto.setClockOut("17:00"); // 9-17 = 480min - 60min = 420min → 7.0h
        dto.setWorkType("NORMAL");
        dto.setStatus("出勤");

        AttendanceDTO result = service.update(existingId, dto);
        assertNotNull(result);
        // auto-calc: (480-60) / 6.0 / 10.0 = 7.0
        assertEquals(7.0, result.getTotalHours(), 0.1,
                "9:00-17:00 → 休憩1h → 実働7.0h");
    }

    @Test
    @DisplayName("update: clockIn/Out なし → 指定値を使用")
    void update_withoutClockInOut_usesProvidedValue() {
        Long existingId = allRecords.get(0).getId();

        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(emp1Id);
        dto.setWorkDate(LocalDate.of(2026, 5, 1));
        dto.setWorkHours(8.0);
        dto.setOvertimeHours(0.0);
        dto.setTotalHours(8.0);
        dto.setWorkType("NORMAL");
        dto.setStatus("出勤");

        AttendanceDTO result = service.update(existingId, dto);
        assertNotNull(result);
    }

    @Test
    @DisplayName("update: 存在しないID → RuntimeException")
    void update_notFound_throwsException() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(emp1Id);
        dto.setWorkDate(LocalDate.of(2026, 5, 1));
        dto.setWorkHours(8.0);
        dto.setWorkType("NORMAL");
        dto.setStatus("出勤");

        assertThrows(RuntimeException.class, () -> service.update(99999L, dto),
                "存在しないレコードの更新は例外");
    }

    // ════════════════════ delete ════════════════════

    @Test
    @DisplayName("delete: レコード削除")
    void delete_existingRecord() {
        Long id = allRecords.get(0).getId();
        service.delete(id);
        assertThrows(RuntimeException.class, () -> service.findById(id),
                "削除後は findById で例外");
    }

    // ════════════════════ CSV出力: 社員名確認 (ID動的解決後) ════════════════════

    @Test
    @DisplayName("exportCsv ①補: 社員名と社員コードがCSVに含まれる")
    void exportCsv_verifyEmployeeNameAppears() {
        String csv = service.exportCsv(2026, 5, emp1Id);
        assertNotNull(csv);
        assertTrue(csv.contains("山田 太郎"), "社員名 山田 太郎 がCSVに含まれているべき");
    }
}
