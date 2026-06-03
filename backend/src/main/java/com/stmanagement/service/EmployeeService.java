package com.stmanagement.service;

import com.stmanagement.dto.BankAccountDTO;
import com.stmanagement.dto.EmployeeDTO;
import com.stmanagement.model.BankAccount;
import com.stmanagement.model.Employee;
import com.stmanagement.model.EmployeeAttachment;
import com.stmanagement.repository.BankAccountRepository;
import com.stmanagement.repository.EmployeeAttachmentRepository;
import com.stmanagement.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BankAccountRepository bankAccountRepository;
    private final EmployeeAttachmentRepository attachmentRepository;
    private final EntityManager entityManager;
    private final Path uploadDir = Paths.get("uploads");

    public Page<EmployeeDTO> findAll(String keyword, String department, int page, int size) {
        Specification<Employee> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String p = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("employeeCode")), p),
                        cb.like(cb.lower(root.get("name")), p)));
            }
            if (StringUtils.hasText(department))
                predicates.add(cb.equal(root.get("department"), department));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return employeeRepository.findAll(spec, PageRequest.of(page, size, Sort.by("employeeCode").ascending()))
                .map(this::toDTO);
    }

    public EmployeeDTO findById(Long id) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("社員が見つかりません: " + id));
        EmployeeDTO dto = toDTO(e);
        dto.setAttachments(attachmentRepository.findByEmployeeId(id).stream()
                .map(a -> EmployeeDTO.AttachmentInfo.builder()
                        .id(a.getId()).fileName(a.getFileName())
                        .filePath(a.getFilePath()).fileSize(a.getFileSize()).build())
                .collect(Collectors.toList()));
        dto.setBankAccounts(bankAccountRepository
                .findAll((Specification<BankAccount>) (root, q, cb) -> cb.equal(root.get("employeeId"), id))
                .stream().map(ba -> BankAccountDTO.builder().id(ba.getId()).torihikiNo(ba.getTorihikiNo())
                        .bankName(ba.getBankName()).branchCode(ba.getBranchCode())
                        .accountType(ba.getAccountType()).accountNumber(ba.getAccountNumber())
                        .accountHolder(ba.getAccountHolder()).build())
                .collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public EmployeeDTO create(EmployeeDTO dto) {
        Employee e = toEntity(dto);
        e.setEmployeeCode(null);
        e = employeeRepository.save(e);
        entityManager.flush();
        entityManager.refresh(e);
        return toDTO(e);
    }

    @Transactional
    public EmployeeDTO update(Long id, EmployeeDTO dto) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("社員が見つかりません: " + id));
        e.setName(dto.getName()); e.setEmail(dto.getEmail()); e.setPhone(dto.getPhone());
        e.setJapanAddress(dto.getJapanAddress()); e.setChinaAddress(dto.getChinaAddress());
        e.setChinaPhone(dto.getChinaPhone()); e.setChinaEmergencyContact(dto.getChinaEmergencyContact());
        e.setDepartment(dto.getDepartment()); e.setPosition(dto.getPosition());
        e.setJoinDate(dto.getJoinDate()); e.setBirthDate(dto.getBirthDate());
        return toDTO(employeeRepository.save(e));
    }

    @Transactional
    public void delete(Long id) {
        if (!employeeRepository.existsById(id))
            throw new RuntimeException("社員が見つかりません: " + id);
        attachmentRepository.deleteByEmployeeId(id);
        employeeRepository.deleteById(id);
    }

    @Transactional
    public List<EmployeeDTO.AttachmentInfo> uploadFiles(Long id, MultipartFile[] files) throws IOException {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("社員が見つかりません: " + id));
        Files.createDirectories(uploadDir);
        List<EmployeeDTO.AttachmentInfo> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String name = "emp_" + id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path target = uploadDir.resolve(name);
            Files.copy(file.getInputStream(), target);
            EmployeeAttachment att = EmployeeAttachment.builder()
                    .employeeId(id).fileName(file.getOriginalFilename())
                    .filePath(target.toString()).fileSize(file.getSize()).build();
            att = attachmentRepository.save(att);
            results.add(EmployeeDTO.AttachmentInfo.builder()
                    .id(att.getId()).fileName(att.getFileName())
                    .filePath(att.getFilePath()).fileSize(att.getFileSize()).build());
        }
        return results;
    }

    public Resource downloadAttachment(Long attachmentId) {
        EmployeeAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("添付ファイルが見つかりません: " + attachmentId));
        Resource r = new FileSystemResource(Paths.get(att.getFilePath()));
        if (!r.exists()) throw new RuntimeException("ファイルが存在しません");
        return r;
    }

    public String getAttachmentFileName(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .map(EmployeeAttachment::getFileName).orElse("download");
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        EmployeeAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("添付ファイルが見つかりません"));
        try { Files.deleteIfExists(Paths.get(att.getFilePath())); } catch (IOException ignored) {}
        attachmentRepository.delete(att);
    }

    @Transactional
    public int batchImport(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), "UTF-8");
        String[] lines = content.split("\\r?\\n");
        int count = 0;
        for (int i = 1; i < lines.length; i++) {
            String[] cols = lines[i].split(",");
            if (cols.length < 3) continue;
            try {
                Employee e = new Employee();
                e.setName(cols[0].trim()); e.setEmail(cols[1].trim()); e.setDepartment(cols[2].trim());
                if (cols.length > 3) e.setPosition(cols[3].trim());
                if (cols.length > 4) e.setJapanAddress(cols[4].trim());
                if (cols.length > 5) e.setChinaAddress(cols[5].trim());
                if (cols.length > 6) e.setPhone(cols[6].trim());
                e.setEmployeeCode(null);
                employeeRepository.save(e); count++;
            } catch (Exception ignored) {}
        }
        return count;
    }

    private EmployeeDTO toDTO(Employee e) {
        return EmployeeDTO.builder().id(e.getId()).employeeCode(e.getEmployeeCode())
                .name(e.getName()).email(e.getEmail()).phone(e.getPhone())
                .japanAddress(e.getJapanAddress()).chinaAddress(e.getChinaAddress())
                .chinaPhone(e.getChinaPhone()).chinaEmergencyContact(e.getChinaEmergencyContact())
                .torihikiNo(e.getTorihikiNo())
                .department(e.getDepartment()).position(e.getPosition())
                .joinDate(e.getJoinDate()).birthDate(e.getBirthDate()).build();
    }

    private Employee toEntity(EmployeeDTO d) {
        return Employee.builder().name(d.getName()).email(d.getEmail()).phone(d.getPhone())
                .japanAddress(d.getJapanAddress()).chinaAddress(d.getChinaAddress())
                .chinaPhone(d.getChinaPhone()).chinaEmergencyContact(d.getChinaEmergencyContact())
                .department(d.getDepartment()).position(d.getPosition())
                .joinDate(d.getJoinDate()).birthDate(d.getBirthDate()).build();
    }
}
