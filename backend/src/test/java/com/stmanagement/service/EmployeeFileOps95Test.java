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
class EmployeeFileOps95Test {
    @Mock private EmployeeRepository empRepo;
    @Mock private EmployeeAttachmentRepository attRepo;
    @Mock private BankAccountRepository baRepo;
    @Mock private EntityManager em;
    @InjectMocks private EmployeeService service;
    @TempDir Path tempDir;
    private Employee emp;

    @BeforeEach void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setEmployeeCode("EMP0001"); emp.setName("山田太郎");
        emp.setDepartment("営業部"); emp.setEmail("t@t.com"); emp.setPhone("090-1111");
        emp.setJapanAddress("東京都"); emp.setStatus("在職"); emp.setJoinDate(LocalDate.of(2020,4,1));
    }

    @Test void uploadFiles_savesToDisk() throws Exception {
        when(empRepo.findById(1L)).thenReturn(Optional.of(emp));
        MockMultipartFile f1 = new MockMultipartFile("files","a.pdf","application/pdf","abc".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files","b.pdf","application/pdf","defg".getBytes());
        EmployeeAttachment saved = new EmployeeAttachment(); saved.setId(1L); saved.setFileName("a.pdf"); saved.setFileSize(3L);
        when(attRepo.save(any())).thenReturn(saved);
        List<EmployeeDTO.AttachmentInfo> r = service.uploadFiles(1L, new MultipartFile[]{f1,f2});
        assertEquals(2, r.size()); assertEquals("a.pdf", r.get(0).getFileName());
    }

    @Test void batchImport_writesToDisk() throws Exception {
        String csv = "EMP0601,花子95,総務部,h95@t.com\nEMP0602,健太95,経理部,k95@t.com";
        MockMultipartFile f = new MockMultipartFile("file","i.csv","text/csv",csv.getBytes("UTF-8"));
        when(empRepo.findAll()).thenReturn(Collections.emptyList()); when(empRepo.save(any())).thenReturn(emp);
        assertEquals(2, service.batchImport(f));
    }

    @Test void deleteAttachment_removesFile() throws Exception {
        Path tf = tempDir.resolve("del95.pdf"); Files.write(tf, "test".getBytes());
        EmployeeAttachment a = new EmployeeAttachment(); a.setId(1L); a.setEmployeeId(1L);
        a.setFileName("del95.pdf"); a.setFilePath(tf.toString());
        when(attRepo.findById(1L)).thenReturn(Optional.of(a)); doNothing().when(attRepo).deleteById(1L);
        service.deleteAttachment(1L); verify(attRepo).deleteById(1L);
    }

    @Test void exportCsv_returnsContent() {
        when(empRepo.findAll()).thenReturn(Collections.singletonList(emp));
        assertTrue(service.exportCsv().contains("EMP0001"));
    }

    @Test void getAttachmentFileName_works() {
        EmployeeAttachment a = new EmployeeAttachment(); a.setId(1L); a.setFileName("myfile95.pdf");
        when(attRepo.findById(1L)).thenReturn(Optional.of(a));
        assertEquals("myfile95.pdf", service.getAttachmentFileName(1L));
    }
}
