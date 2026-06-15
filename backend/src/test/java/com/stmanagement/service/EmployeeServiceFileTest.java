package com.stmanagement.service;

import com.stmanagement.dto.EmployeeDTO;
import com.stmanagement.model.Employee;
import com.stmanagement.model.EmployeeAttachment;
import com.stmanagement.repository.BankAccountRepository;
import com.stmanagement.repository.EmployeeAttachmentRepository;
import com.stmanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceFileTest {
    @Mock private EmployeeRepository empRepo;
    @Mock private EmployeeAttachmentRepository attRepo;
    @Mock private BankAccountRepository baRepo;
    @Mock private EntityManager em;
    @InjectMocks private EmployeeService service;
    @TempDir Path tempDir;
    private Employee emp;
    @BeforeEach void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setEmployeeCode("EMP0001");
        emp.setName("山田 太郎"); emp.setDepartment("営業部"); emp.setPosition("課長");
        emp.setEmail("yamada@test.com"); emp.setPhone("090-1111-2222");
        emp.setJapanAddress("東京都新宿区"); emp.setStatus("在職");
        emp.setJoinDate(LocalDate.of(2020,4,1));
    }

    @Test void testUploadFiles_multiple() throws Exception {
        when(empRepo.findById(1L)).thenReturn(Optional.of(emp));
        MockMultipartFile f1 = new MockMultipartFile("files","a.pdf","application/pdf","a".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files","b.pdf","application/pdf","bb".getBytes());
        EmployeeAttachment a1 = new EmployeeAttachment();
        a1.setId(1L); a1.setEmployeeId(1L); a1.setFileName("a.pdf"); a1.setFileSize(1L);
        when(attRepo.save(any())).thenReturn(a1);
        List<EmployeeDTO.AttachmentInfo> r = service.uploadFiles(1L, new MultipartFile[]{f1,f2});
        assertNotNull(r); assertEquals(2, r.size());
    }

    @Test void testBatchImport_success() throws Exception {
        String csv = "EMP0006,田中 花子,技術部,tanaka@test.com";
        MockMultipartFile file = new MockMultipartFile("file","import.csv","text/csv",csv.getBytes("UTF-8"));
        when(empRepo.findAll()).thenReturn(Collections.emptyList());
        when(empRepo.save(any(Employee.class))).thenReturn(emp);
        int count = service.batchImport(file);
        assertEquals(1, count);
    }

    @Test void testDownloadAttachment_notFound() {
        when(attRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.downloadAttachment(99L));
    }

    @Test void testGetAttachmentFileName_notFound() {
        when(attRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getAttachmentFileName(99L));
    }

    @Test void testExportCsv() {
        when(empRepo.findAll()).thenReturn(Collections.singletonList(emp));
        String csv = service.exportCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("EMP0001"));
    }

    @Test void testUploadFiles_single() throws Exception {
        when(empRepo.findById(1L)).thenReturn(Optional.of(emp));
        MockMultipartFile f1 = new MockMultipartFile("files","doc.pdf","application/pdf","x".getBytes());
        EmployeeAttachment a1 = new EmployeeAttachment();
        a1.setId(1L); a1.setEmployeeId(1L); a1.setFileName("doc.pdf"); a1.setFileSize(1L);
        when(attRepo.save(any())).thenReturn(a1);
        List<EmployeeDTO.AttachmentInfo> r = service.uploadFiles(1L, new MultipartFile[]{f1});
        assertNotNull(r); assertEquals(1, r.size());
    }
}
