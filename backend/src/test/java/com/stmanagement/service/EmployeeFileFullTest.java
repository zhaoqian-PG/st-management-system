package com.stmanagement.service;

import com.stmanagement.dto.EmployeeDTO;
import com.stmanagement.model.*;
import com.stmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeFileFullTest {
    @Mock private EmployeeRepository empRepo;
    @Mock private EmployeeAttachmentRepository attRepo;
    @Mock private BankAccountRepository baRepo;
    @Mock private EntityManager em;
    @InjectMocks private EmployeeService service;
    @TempDir Path tempDir;
    private Employee emp;

    @BeforeEach void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setEmployeeCode("EMP0001");
        emp.setName("山田太郎"); emp.setDepartment("営業部"); emp.setEmail("t@t.com");
        emp.setPhone("090-1111"); emp.setJapanAddress("東京都"); emp.setStatus("在職");
        emp.setJoinDate(LocalDate.of(2020,4,1));
    }

    @Test void uploadFiles_twoFiles() throws Exception {
        when(empRepo.findById(1L)).thenReturn(Optional.of(emp));
        MockMultipartFile f1 = new MockMultipartFile("f","a.pdf","application/pdf","abc".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("f","b.pdf","application/pdf","defg".getBytes());
        EmployeeAttachment a1 = new EmployeeAttachment(); a1.setFileName("a.pdf"); a1.setFileSize(3L);
        EmployeeAttachment a2 = new EmployeeAttachment(); a2.setFileName("b.pdf"); a2.setFileSize(4L);
        when(attRepo.save(any())).thenReturn(a1, a2);
        List<EmployeeDTO.AttachmentInfo> r = service.uploadFiles(1L, new MultipartFile[]{f1, f2});
        assertEquals(2, r.size());
    }

    @Test void uploadFiles_empty() throws Exception {
        when(empRepo.findById(1L)).thenReturn(Optional.of(emp));
        List<EmployeeDTO.AttachmentInfo> r = service.uploadFiles(1L, new MultipartFile[]{});
        assertTrue(r.isEmpty());
    }

    @Test void batchImport_createsNew() throws Exception {
        String csv = "EMP0301,新規太郎,総務部,new1@t.com\nEMP0302,新規花子,経理部,new2@t.com";
        MockMultipartFile f = new MockMultipartFile("f","i.csv","text/csv",csv.getBytes("UTF-8"));
        when(empRepo.findAll()).thenReturn(Collections.emptyList());
        when(empRepo.save(any())).thenReturn(emp);
        assertEquals(2, service.batchImport(f));
    }

    @Test void batchImport_skipsExisting() throws Exception {
        String csv = "EMP0001,既存太郎,営業部,exist@t.com";
        MockMultipartFile f = new MockMultipartFile("f","i.csv","text/csv",csv.getBytes("UTF-8"));
        when(empRepo.findAll()).thenReturn(Collections.singletonList(emp));
        assertEquals(0, service.batchImport(f));
    }

    @Test void deleteAttachment_deletesFile() throws Exception {
        Path tf = tempDir.resolve("del.pdf");
        Files.write(tf, "test".getBytes());
        EmployeeAttachment att = new EmployeeAttachment();
        att.setId(1L); att.setEmployeeId(1L); att.setFileName("del.pdf"); att.setFilePath(tf.toString());
        when(attRepo.findById(1L)).thenReturn(Optional.of(att));
        doNothing().when(attRepo).deleteById(1L);
        service.deleteAttachment(1L);
        verify(attRepo).deleteById(1L);
    }

    @Test void getAttachmentFileName_works() {
        EmployeeAttachment att = new EmployeeAttachment();
        att.setId(1L); att.setFileName("myfile.pdf");
        when(attRepo.findById(1L)).thenReturn(Optional.of(att));
        assertEquals("myfile.pdf", service.getAttachmentFileName(1L));
    }

    @Test void downloadAttachment_notFound() {
        when(attRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.downloadAttachment(99L));
    }

    @Test void exportCsv_hasContent() {
        when(empRepo.findAll()).thenReturn(Collections.singletonList(emp));
        String csv = service.exportCsv();
        assertNotNull(csv); assertTrue(csv.contains("EMP0001"));
    }
}
