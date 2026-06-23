package com.stmanagement.service;

import com.stmanagement.dto.EmployeeDTO;
import com.stmanagement.model.*;
import com.stmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    // ──────────── findAll 既存 ────────────

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

    // ──────────── findAll: torihikiNo 分岐 (銀行口座あり/なし) ────────────

    @Test void testFindAll_withBanks_populatesTorihikiNo() {
        when(empRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        // 銀行口座あり → torihikiNo が設定される
        BankAccount ba = new BankAccount(); ba.setId(1L); ba.setEmployeeId(1L);
        ba.setBankName("三菱UFJ銀行"); ba.setBranchCode("001");
        when(baRepo.findAll(any(Specification.class))).thenReturn(Collections.singletonList(ba));

        Page<EmployeeDTO> r = service.findAll(null, null, false, 0, 10);
        assertNotNull(r);
        EmployeeDTO dto = r.getContent().get(0);
        assertNotNull(dto.getTorihikiNo(), "銀行口座がある場合 torihikiNo が設定されるべき");
        assertTrue(dto.getTorihikiNo().contains("三菱UFJ銀行"));
        assertTrue(dto.getTorihikiNo().contains("001"));
    }

    @Test void testFindAll_withoutBanks_torihikiNoRemainsNull() {
        when(empRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        when(baRepo.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        Page<EmployeeDTO> r = service.findAll(null, null, false, 0, 10);
        assertNotNull(r);
        EmployeeDTO dto = r.getContent().get(0);
        assertNull(dto.getTorihikiNo(), "銀行口座がない場合 torihikiNo は null");
    }

    // ──────────── findAll: Specification 分岐全組合せ ────────────

    @Test void testFindAll_keywordOnly() {
        when(empRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        Page<EmployeeDTO> r = service.findAll("山田", null, true, 0, 10);
        assertNotNull(r);
    }

    @Test void testFindAll_departmentOnly() {
        when(empRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        Page<EmployeeDTO> r = service.findAll(null, "営業部", true, 0, 10);
        assertNotNull(r);
    }

    @Test void testFindAll_includeResignedTrue() {
        when(empRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        Page<EmployeeDTO> r = service.findAll(null, null, true, 0, 10);
        assertNotNull(r);
    }

    @Test void testFindAll_includeResignedFalse() {
        when(empRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        Page<EmployeeDTO> r = service.findAll(null, null, false, 0, 10);
        assertNotNull(r);
    }

    @Test void testFindAll_keywordDepartmentIncludeResigned() {
        when(empRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        Page<EmployeeDTO> r = service.findAll("佐藤", "開発部", false, 0, 10);
        assertNotNull(r);
    }

    // ──────────── update 既存 ────────────

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

    // ──────────── update: NotFound / leaveDate → 離職 ────────────

    @Test void testUpdate_NotFound() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("更新"); dto.setStatus("在職");
        when(empRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.update(999L, dto),
                "存在しない社員の更新 → RuntimeException");
    }

    @Test void testUpdate_withLeaveDate_setsStatusResigned() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("退職太郎"); dto.setStatus("在職");
        dto.setLeaveDate(LocalDate.of(2026, 3, 31)); // 退職日あり → "離職"

        when(empRepo.findById(1L)).thenReturn(Optional.of(emp));
        when(empRepo.save(any(Employee.class))).thenReturn(emp);

        EmployeeDTO r = service.update(1L, dto);
        assertNotNull(r);
        // status は toDTO 内で leaveDate != null ? "離職" : "在職" で再設定される
        assertEquals("離職", r.getStatus(), "退職日あり → status は '離職'");
    }

    // ──────────── create: leaveDate → 離職 ────────────

    @Test void testCreate_withLeaveDate() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("退職花子"); dto.setDepartment("人事部");
        dto.setLeaveDate(LocalDate.of(2026, 6, 30));
        when(empRepo.save(any(Employee.class))).thenReturn(emp);
        // entityManager.refresh() は stubbing 不要（em は mock, refresh は void）

        EmployeeDTO r = service.create(dto);
        assertNotNull(r);
    }

    // ──────────── delete 既存 + NotFound ────────────

    @Test void testDelete_byId() {
        when(empRepo.existsById(1L)).thenReturn(true);
        doNothing().when(empRepo).deleteById(1L);
        service.delete(1L);
        verify(empRepo).deleteById(1L);
    }

    @Test void testDelete_NotFound() {
        when(empRepo.existsById(999L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.delete(999L),
                "存在しない社員の削除 → RuntimeException");
    }

    // ──────────── exportCsv: 全フィールドあり (null分岐網羅) ────────────

    @Test void testExportCsv_allFields_populated() {
        Employee fullEmp = new Employee();
        fullEmp.setId(3L); fullEmp.setEmployeeCode("EMP0003");
        fullEmp.setName("全部入り"); fullEmp.setEmail("all@test.com");
        fullEmp.setPhone("03-1234-5678"); fullEmp.setDepartment("全部署");
        fullEmp.setPosition("全役職"); fullEmp.setStatus("在職");
        fullEmp.setJoinDate(LocalDate.of(2019, 4, 1));
        fullEmp.setBirthDate(LocalDate.of(1990, 1, 15));
        fullEmp.setLeaveDate(LocalDate.of(2026, 12, 31));
        fullEmp.setJapanAddress("日本全部");
        fullEmp.setChinaAddress("中国全部");
        fullEmp.setChinaPhone("+86-1234");
        fullEmp.setChinaEmergencyContact("緊急連絡先太郎");

        when(empRepo.findAll(any(Sort.class))).thenReturn(Collections.singletonList(fullEmp));

        String csv = service.exportCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("1990-01-15"), "生年月日が含まれるべき");
        assertTrue(csv.contains("2026-12-31"), "離職日が含まれるべき");
        assertTrue(csv.contains("中国全部"), "中国住所が含まれるべき");
        assertTrue(csv.contains("+86-1234"), "中国電話が含まれるべき");
        assertTrue(csv.contains("緊急連絡先太郎"), "緊急連絡先が含まれるべき");
    }

    // ──────────── uploadFiles: NotFound / 空ファイル / 複数ファイル ────────────

    @Test void testUploadFiles_NotFound() {
        when(empRepo.findById(999L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                "application/pdf", "data".getBytes());
        assertThrows(RuntimeException.class,
                () -> service.uploadFiles(999L, new MultipartFile[]{file}),
                "存在しない社員 → RuntimeException");
    }

    @Test void testUploadFiles_emptyFile_skipped() throws Exception {
        when(empRepo.findById(1L)).thenReturn(Optional.of(emp));
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf",
                "application/pdf", new byte[0]); // 空ファイル
        List<EmployeeDTO.AttachmentInfo> r = service.uploadFiles(1L, new MultipartFile[]{emptyFile});
        assertNotNull(r);
        assertTrue(r.isEmpty(), "空ファイルはスキップ → 結果リストは空");
    }

    // ──────────── downloadAttachment: 正常 / ファイル不在 ────────────

    @Test void testDownloadAttachment_NotFound() {
        when(attRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.downloadAttachment(999L),
                "添付ファイルが見つからない → RuntimeException");
    }

    @Test void testDownloadAttachment_normal(@TempDir Path tempDir) throws IOException {
        // 実ファイルを作成して正常系をテスト
        Path realFile = tempDir.resolve("real.pdf");
        Files.write(realFile, "test content".getBytes());

        EmployeeAttachment att = new EmployeeAttachment();
        att.setId(1L); att.setEmployeeId(1L);
        att.setFileName("real.pdf"); att.setFilePath(realFile.toString()); att.setFileSize(1024L);

        when(attRepo.findById(1L)).thenReturn(Optional.of(att));

        org.springframework.core.io.Resource r = service.downloadAttachment(1L);
        assertNotNull(r);
        assertTrue(r.exists(), "実ファイルが存在するべき");
    }

    @Test void testDownloadAttachment_fileNotOnDisk() {
        EmployeeAttachment att = new EmployeeAttachment();
        att.setId(1L); att.setFilePath("/nonexistent/path/deleted.pdf");

        when(attRepo.findById(1L)).thenReturn(Optional.of(att));

        assertThrows(RuntimeException.class, () -> service.downloadAttachment(1L),
                "ファイルがディスクに存在しない → RuntimeException");
    }

    // ──────────── getAttachmentFileName: NotFound → "download" ────────────

    @Test void testGetAttachmentFileName_notFound_returnsDownload() {
        when(attRepo.findById(999L)).thenReturn(Optional.empty());
        assertEquals("download", service.getAttachmentFileName(999L),
                "添付ファイル未検出 → 'download' を返す");
    }

    // ──────────── deleteAttachment: 正常 + NotFound ────────────

    @Test void testDeleteAttachment_normal() {
        EmployeeAttachment att = new EmployeeAttachment();
        att.setId(1L); att.setEmployeeId(1L);
        att.setFileName("test.pdf"); att.setFilePath("/tmp/test.pdf");

        when(attRepo.findById(1L)).thenReturn(Optional.of(att));
        doNothing().when(attRepo).delete(att);

        service.deleteAttachment(1L);
        verify(attRepo).delete(att);
    }

    @Test void testDeleteAttachment_NotFound() {
        when(attRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.deleteAttachment(999L),
                "添付ファイルが見つからない → RuntimeException");
    }

    // ──────────── batchImport: 全分岐 ────────────

    @Test void testBatchImport_basic() throws IOException {
        String csv = "名前,メール,部署,役職,日本住所,中国住所,電話\n"
                + "山田太郎,yamada@test.com,営業部,課長,東京都,,090-1111\n"
                + "佐藤花子,sato@test.com,開発部,,大阪府,,080-2222";
        MockMultipartFile file = new MockMultipartFile("file", "import.csv",
                "text/csv", csv.getBytes("UTF-8"));

        when(empRepo.save(any(Employee.class))).thenReturn(emp);

        int count = service.batchImport(file);
        assertEquals(2, count, "2行ともインポートされるべき");
    }

    @Test void testBatchImport_skipShortRows() throws IOException {
        String csv = "名前,メール,部署\n"
                + "山田太郎,yamada@test.com,営業部\n"     // cols=3 → OK
                + "短い行,short\n"                          // cols=2 → skip
                + "佐藤花子,sato@test.com,開発部";          // cols=3 → OK

        MockMultipartFile file = new MockMultipartFile("file", "import.csv",
                "text/csv", csv.getBytes("UTF-8"));

        when(empRepo.save(any(Employee.class))).thenReturn(emp);

        int count = service.batchImport(file);
        assertEquals(2, count, "短い行(2列)はスキップ → 2行のみ");
    }

    @Test void testBatchImport_allColumns_extendsFields() throws IOException {
        // 全7列（cols.length > 6）→ 電話まで設定
        String csv = "名前,メール,部署,役職,日本住所,中国住所,電話\n"
                + "田中,tana@test.com,総務,部長,東京,上海,03-1234";
        MockMultipartFile file = new MockMultipartFile("file", "import.csv",
                "text/csv", csv.getBytes("UTF-8"));

        when(empRepo.save(any(Employee.class))).thenReturn(emp);

        int count = service.batchImport(file);
        assertEquals(1, count);
        verify(empRepo).save(any(Employee.class));
    }

    @Test void testBatchImport_zeroRows() throws IOException {
        String csv = "名前,メール,部署"; // ヘッダーのみ、データ行なし
        MockMultipartFile file = new MockMultipartFile("file", "import.csv",
                "text/csv", csv.getBytes("UTF-8"));

        int count = service.batchImport(file);
        assertEquals(0, count, "データ行なし → 0");
    }

    // ──────────── exportCsv: 全分岐 ────────────

    @Test void testExportCsv_basic() {
        when(empRepo.findAll(any(Sort.class))).thenReturn(Collections.singletonList(emp));

        String csv = service.exportCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("社員番号,氏名,メール"), "ヘッダー行が含まれるべき");
        assertTrue(csv.contains("EMP0001"), "社員番号が含まれるべき");
        assertTrue(csv.contains("山田 太郎"), "氏名が含まれるべき");
        assertTrue(csv.contains("yamada@test.com"), "メールが含まれるべき");
        assertTrue(csv.contains("営業部"), "部署が含まれるべき");
        assertTrue(csv.contains("課長"), "役職が含まれるべき");
        assertTrue(csv.contains("2020-04-01"), "入社日が含まれるべき");
        assertTrue(csv.contains("在職"), "状態が含まれるべき");
    }

    @Test void testExportCsv_nullFields() {
        // すべての nullable フィールドが null
        Employee nullEmp = new Employee();
        nullEmp.setId(2L); nullEmp.setEmployeeCode("EMP0002");
        nullEmp.setName("null太郎"); nullEmp.setStatus("在職");
        // email, phone, department, position, joinDate, birthDate, leaveDate,
        // japanAddress, chinaAddress, chinaPhone, chinaEmergencyContact すべて null

        when(empRepo.findAll(any(Sort.class))).thenReturn(Collections.singletonList(nullEmp));

        String csv = service.exportCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("EMP0002"));
        assertTrue(csv.contains("null太郎"));
        // null フィールドは空文字列になる → NPE しないこと
    }

    @Test void testExportCsv_multipleEmployees() {
        Employee emp2 = new Employee(); emp2.setId(2L); emp2.setEmployeeCode("EMP0002");
        emp2.setName("佐藤 花子"); emp2.setDepartment("開発部"); emp2.setStatus("在職");
        emp2.setJoinDate(LocalDate.of(2021, 6, 1));

        when(empRepo.findAll(any(Sort.class)))
                .thenReturn(Arrays.asList(emp, emp2));

        String csv = service.exportCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("EMP0001") && csv.contains("EMP0002"),
                "全社員のデータが含まれるべき");
    }

    @Test void testExportCsv_empty() {
        when(empRepo.findAll(any(Sort.class))).thenReturn(Collections.emptyList());

        String csv = service.exportCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("社員番号"), "ヘッダーは空でも出力されるべき");
    }

    // ──────────── toDTO: leaveDate → 離職 (findById 経由) ────────────

    @Test void testFindById_resignedEmployee_statusIsResigned() {
        emp.setLeaveDate(LocalDate.of(2026, 3, 31)); // 退職済み
        when(empRepo.findById(1L)).thenReturn(Optional.of(emp));

        EmployeeDTO r = service.findById(1L);
        assertNotNull(r);
        assertEquals("離職", r.getStatus(), "退職日あり → status は '離職'");
    }
}
