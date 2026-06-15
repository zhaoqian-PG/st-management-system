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
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
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
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepo;
    @Mock private EmployeeAttachmentRepository attRepo;
    @Mock private BankAccountRepository baRepo;
    @Mock private EntityManager em;
    @InjectMocks private EmployeeService service;

    private Employee emp;

    @BeforeEach
    void setUp() {
        emp = new Employee(); emp.setId(1L); emp.setEmployeeCode("EMP0001");
        emp.setName("山田 太郎"); emp.setDepartment("営業部"); emp.setPosition("課長");
        emp.setEmail("yamada@example.com"); emp.setPhone("090-1111-2222");
        emp.setJapanAddress("東京都新宿区"); emp.setStatus("在職");
        emp.setJoinDate(LocalDate.of(2020, 4, 1));
    }

    @Test void testFindAll() {
        when(employeeRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(emp)));
        Page<EmployeeDTO> r = service.findAll(null, null, false, 0, 10);
        assertNotNull(r); assertEquals(1, r.getTotalElements());
    }
    @Test void testFindById() {
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));
        EmployeeDTO r = service.findById(1L);
        assertNotNull(r); assertEquals("山田 太郎", r.getName());
    }
    @Test void testFindById_NotFound() {
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }
    @Test void testCreate() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("新規"); dto.setDepartment("技術部");
        when(employeeRepo.save(any(Employee.class))).thenReturn(emp);
        assertNotNull(service.create(dto));
    }
    @Test void testUpdate() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("更新"); dto.setStatus("在職");
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));
        when(employeeRepo.save(any(Employee.class))).thenReturn(emp);
        assertNotNull(service.update(1L, dto));
    }
    @Test void testDelete() {
        when(employeeRepo.existsById(1L)).thenReturn(true);
        doNothing().when(employeeRepo).deleteById(1L);
        service.delete(1L);
        verify(employeeRepo).deleteById(1L);
    }

    @Test void testUploadFiles() throws Exception {
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(emp));
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        when(attRepo.save(any(EmployeeAttachment.class))).thenReturn(new EmployeeAttachment());

        List<EmployeeDTO.AttachmentInfo> r = service.uploadFiles(1L, new MultipartFile[]{file});
        assertNotNull(r);
        assertEquals(1, r.size());
    }

    @Test void testGetAttachmentFileName() {
        EmployeeAttachment att = new EmployeeAttachment();
        att.setId(1L); att.setEmployeeId(1L); att.setFileName("test.pdf");
        att.setFilePath("uploads/test.pdf"); att.setFileSize(1024L);
        when(attRepo.findById(1L)).thenReturn(Optional.of(att));
        assertEquals("test.pdf", service.getAttachmentFileName(1L));
    }

    @Test void testDownloadAttachment_NotFound() {
        when(attRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.downloadAttachment(99L));
    }
}
