package com.stmanagement.controller;

import com.stmanagement.common.ApiResponse;
import com.stmanagement.dto.EmployeeDTO;
import com.stmanagement.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 10);
        Page<EmployeeDTO> r = employeeService.findAll(keyword, department, page, size);
        return ResponseEntity.ok(ApiResponse.success(r));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.status(201).body(ApiResponse.success(employeeService.create(dto), "社員を登録しました"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.update(id, dto), "社員情報を更新しました"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "社員を削除しました"));
    }

    @PostMapping("/{id}/upload")
    public ResponseEntity<?> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            String path = employeeService.uploadFile(id, file);
            return ResponseEntity.ok(ApiResponse.success(path, "ファイルをアップロードしました"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("アップロード失敗: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/batch-import")
    public ResponseEntity<?> batchImport(@RequestParam("file") MultipartFile file) {
        try {
            int count = employeeService.batchImport(file);
            return ResponseEntity.ok(ApiResponse.success(count, count + "件の社員を登録しました"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("インポート失敗: " + e.getMessage(), 400));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id) {
        EmployeeDTO dto = employeeService.findById(id);
        if (dto.getAttachmentPath() == null || dto.getAttachmentPath().isEmpty())
            return ResponseEntity.notFound().build();
        Resource resource = new FileSystemResource(Paths.get(dto.getAttachmentPath()));
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
