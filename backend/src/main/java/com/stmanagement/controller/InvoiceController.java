package com.stmanagement.controller;

import com.stmanagement.common.ApiResponse;
import com.stmanagement.dto.InvoiceDTO;
import com.stmanagement.service.InvoiceService;
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

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 10);
        Page<InvoiceDTO> r = invoiceService.findAll(year, month, customerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(r));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody InvoiceDTO dto) {
        return ResponseEntity.status(201).body(ApiResponse.success(invoiceService.create(dto), "請求書を登録しました"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody InvoiceDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.update(id, dto), "請求書を更新しました"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "請求書を削除しました"));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<?> uploadDocument(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(ApiResponse.success(invoiceService.uploadDocument(id, file), "ファイルをアップロードしました"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("アップロード失敗: " + e.getMessage(), 400));
        }
    }

    @GetMapping("/documents/{docId}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable Long docId) {
        try {
            Resource resource = invoiceService.getDocumentFile(docId);
            String fileName = invoiceService.getDocumentFileName(docId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/documents/{docId}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long docId) {
        invoiceService.deleteDocument(docId);
        return ResponseEntity.ok(ApiResponse.success(null, "ファイルを削除しました"));
    }
}
