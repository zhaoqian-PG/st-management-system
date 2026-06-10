package com.stmanagement.controller;

import com.stmanagement.common.ApiResponse;
import com.stmanagement.service.SupplierOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/supplier-order") @RequiredArgsConstructor
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
}
