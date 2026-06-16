package com.stmanagement.service;

import com.stmanagement.dto.EmployeeDTO;
import com.stmanagement.model.*;
import com.stmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeFullTest {
    @Mock private EmployeeRepository eRepo;
    @Mock private EmployeeAttachmentRepository aRepo;
    @Mock private BankAccountRepository bRepo;
    @Mock private EntityManager em;
    @InjectMocks private EmployeeService service;

    private Employee emp;

    @BeforeEach void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setEmployeeCode("EMP0001"); emp.setName("山田");
        emp.setDepartment("営業部"); emp.setPosition("課長"); emp.setEmail("t@t.com");
        emp.setPhone("090-1111"); emp.setJapanAddress("東京都"); emp.setStatus("在職");
        emp.setJoinDate(LocalDate.of(2020,4,1));
    }

    @Test void findAll_basic() {
        when(eRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        assertEquals(1, service.findAll(null,null,false,0,10).getTotalElements());
    }
    @Test void findAll_withKeyword() {
        when(eRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        assertEquals(1, service.findAll("山田","営業部",true,0,10).getTotalElements());
    }
    @Test void findAll_empty() {
        when(eRepo.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        assertTrue(service.findAll(null,null,false,0,10).isEmpty());
    }
    @Test void findById() { when(eRepo.findById(1L)).thenReturn(Optional.of(emp)); assertEquals("山田",service.findById(1L).getName()); }
    @Test void findById_notFound() { when(eRepo.findById(99L)).thenReturn(Optional.empty()); assertThrows(RuntimeException.class,()->service.findById(99L)); }
    @Test void create() { EmployeeDTO d = new EmployeeDTO(); d.setName("新規"); d.setDepartment("技術部"); d.setJoinDate(LocalDate.now()); when(eRepo.save(any())).thenReturn(emp); assertNotNull(service.create(d)); }
    @Test void update() { EmployeeDTO d = new EmployeeDTO(); d.setName("更新"); d.setDepartment("経理部"); d.setStatus("在職"); d.setPhone("090-9999"); when(eRepo.findById(1L)).thenReturn(Optional.of(emp)); when(eRepo.save(any())).thenReturn(emp); assertNotNull(service.update(1L,d)); }
    @Test void update_notFound() { when(eRepo.findById(99L)).thenReturn(Optional.empty()); assertThrows(RuntimeException.class,()->service.update(99L,new EmployeeDTO())); }
    @Test void delete() { when(eRepo.existsById(1L)).thenReturn(true); doNothing().when(eRepo).deleteById(1L); service.delete(1L); verify(eRepo).deleteById(1L); }
    @Test void uploadFiles_single() throws Exception {
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        MockMultipartFile f = new MockMultipartFile("f","a.pdf","application/pdf","abc".getBytes());
        EmployeeAttachment att = new EmployeeAttachment(); att.setFileName("a.pdf"); att.setFileSize(3L);
        when(aRepo.save(any())).thenReturn(att);
        assertEquals(1, service.uploadFiles(1L, new org.springframework.web.multipart.MultipartFile[]{f}).size());
    }
    @Test void uploadFiles_empty() throws Exception {
        when(eRepo.findById(1L)).thenReturn(Optional.of(emp));
        assertTrue(service.uploadFiles(1L, new org.springframework.web.multipart.MultipartFile[]{}).isEmpty());
    }
    @Test void getAttachmentFileName() { EmployeeAttachment a = new EmployeeAttachment(); a.setId(1L); a.setFileName("doc.pdf"); when(aRepo.findById(1L)).thenReturn(Optional.of(a)); assertEquals("doc.pdf",service.getAttachmentFileName(1L)); }
    @Test void getAttachmentFileName_notFound() { when(aRepo.findById(99L)).thenReturn(Optional.empty()); assertThrows(RuntimeException.class,()->service.getAttachmentFileName(99L)); }
    @Test void deleteAttachment() { EmployeeAttachment a = new EmployeeAttachment(); a.setId(1L); a.setFileName("d.pdf"); a.setFilePath("uploads/d.pdf"); when(aRepo.findById(1L)).thenReturn(Optional.of(a)); doNothing().when(aRepo).deleteById(1L); service.deleteAttachment(1L); verify(aRepo).deleteById(1L); }
    @Test void downloadAttachment_notFound() { when(aRepo.findById(99L)).thenReturn(Optional.empty()); assertThrows(RuntimeException.class,()->service.downloadAttachment(99L)); }
    @Test void batchImport_new() throws Exception {
        String csv = "EMP4001,花子,総務部,h@t.com\nEMP4002,健太,経理部,k@t.com";
        MockMultipartFile f = new MockMultipartFile("f","i.csv","text/csv",csv.getBytes("UTF-8"));
        when(eRepo.findAll()).thenReturn(Collections.emptyList()); when(eRepo.save(any())).thenReturn(emp);
        assertEquals(2, service.batchImport(f));
    }
    @Test void exportCsv() { when(eRepo.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(Collections.singletonList(emp)); assertTrue(service.exportCsv().contains("EMP0001")); }
}
