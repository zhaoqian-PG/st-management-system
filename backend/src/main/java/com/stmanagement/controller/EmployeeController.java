package com.stmanagement.controller;

import com.stmanagement.common.ApiResponse;
import com.stmanagement.dto.EmployeeDTO;
import com.stmanagement.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "false") boolean includeResigned,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 10);
        Page<EmployeeDTO> r = employeeService.findAll(keyword, department, includeResigned, page, size);
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
    public ResponseEntity<?> upload(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files) {
        try {
            List<EmployeeDTO.AttachmentInfo> result = employeeService.uploadFiles(id, files.toArray(new MultipartFile[0]));
            return ResponseEntity.ok(ApiResponse.success(result, result.size() + "件のファイルをアップロードしました"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("アップロード失敗: " + e.getMessage(), 400));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportCsv() {
        String csv = employeeService.exportCsv();
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee_list.csv")
                .body(csv);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<?> download(@PathVariable Long attachmentId) {
        Resource resource = employeeService.downloadAttachment(attachmentId);
        String fileName = employeeService.getAttachmentFileName(attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long attachmentId) {
        employeeService.deleteAttachment(attachmentId);
        return ResponseEntity.ok(ApiResponse.success(null, "ファイルを削除しました"));
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
}
