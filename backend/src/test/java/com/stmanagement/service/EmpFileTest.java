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

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpFileTest {
    @Mock private EmployeeRepository eRepo;
    @Mock private EmployeeAttachmentRepository aRepo;
    @Mock private BankAccountRepository bRepo;
    @Mock private EntityManager em;
    @InjectMocks private EmployeeService svc;
    @TempDir java.nio.file.Path dir;
    private Employee emp;

    @BeforeEach void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setEmployeeCode("EMP0001");
        emp.setName("山田"); emp.setDepartment("営業部"); emp.setEmail("t@t.com");
        emp.setPhone("090-1111"); emp.setJapanAddress("東京都"); emp.setStatus("在職");
        emp.setJoinDate(LocalDate.of(2020,4,1));
    }

    @Test void upload_twoFiles() throws Exception {
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        MockMultipartFile f1 = new MockMultipartFile("f","a.pdf","application/pdf","abc".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("f","b.pdf","application/pdf","def".getBytes());
        EmployeeAttachment a = new EmployeeAttachment(); a.setFileName("a.pdf"); a.setFileSize(3L);
        when(aRepo.save(any())).thenReturn(a);
        List<EmployeeDTO.AttachmentInfo> r = svc.uploadFiles(1L,
            new org.springframework.web.multipart.MultipartFile[]{f1, f2});
        assertEquals(2, r.size());
    }

    @Test void upload_empty() throws Exception {
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        assertEquals(0, svc.uploadFiles(1L, new org.springframework.web.multipart.MultipartFile[]{}).size());
    }

    @Test void batchImport_twoNew() throws Exception {
        String csv = "EMP7001,花子,総務部,h@t.com\nEMP7002,健太,経理部,k@t.com";
        MockMultipartFile f = new MockMultipartFile("f","i.csv","text/csv",csv.getBytes("UTF-8"));
        when(eRepo.findAll()).thenReturn(Collections.emptyList());
        when(eRepo.save(any())).thenReturn(emp);
        assertEquals(2, svc.batchImport(f));
    }

    @Test void batchImport_skipExisting() throws Exception {
        String csv = "EMP0001,既存,営業部,e@t.com";
        MockMultipartFile f = new MockMultipartFile("f","i.csv","text/csv",csv.getBytes("UTF-8"));
        when(eRepo.findAll()).thenReturn(Collections.singletonList(emp));
        assertEquals(0, svc.batchImport(f));
    }

    @Test void getAttachmentFileName_ok() {
        EmployeeAttachment a = new EmployeeAttachment(); a.setId(1L); a.setFileName("my.pdf");
        when(aRepo.findById(1L)).thenReturn(Optional.of(a));
        assertEquals("my.pdf", svc.getAttachmentFileName(1L));
    }

    @Test void exportCsv_ok() {
        when(eRepo.findAll(any(org.springframework.data.domain.Sort.class)))
            .thenReturn(Collections.singletonList(emp));
        assertTrue(svc.exportCsv().contains("EMP0001"));
    }
}
