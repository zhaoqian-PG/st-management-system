package com.stmanagement.controller;

import com.stmanagement.common.ApiResponse;
import com.stmanagement.service.SupplierOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.util.Map;

@RestController @RequestMapping("/api/supplier-order") @RequiredArgsConstructor @Slf4j
public class SupplierOrderController {
    private final SupplierOrderService service;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        Page<Map<String,Object>> r = service.findAll(page, size);
        return ResponseEntity.ok(ApiResponse.success(r));
    }

    @GetMapping("/{id}") public ResponseEntity<?> getById(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success(service.findById(id))); }
    @PostMapping public ResponseEntity<?> create(@RequestBody Map<String,Object> dto) { return ResponseEntity.status(201).body(ApiResponse.success(service.create(dto),"発注書を登録しました")); }
    @PutMapping("/{id}") public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String,Object> dto) { return ResponseEntity.ok(ApiResponse.success(service.update(id,dto),"発注書を更新しました")); }
    @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(ApiResponse.success(null,"発注書を削除しました")); }

    @GetMapping("/export/{id}")
    public ResponseEntity<?> exportPdf(@PathVariable Long id) {
        try {
            byte[] pdf = service.exportPdf(id);
            String filename = "hacchusho_" + id + ".pdf";
            String encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                    .body(pdf);
        } catch (Exception e) {
            log.error("PDF export failed for supplier order id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("PDF出力に失敗しました: " + e.getMessage(), 400));
        }
    }
}
