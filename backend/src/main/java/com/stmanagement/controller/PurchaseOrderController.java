package com.stmanagement.controller;

import com.stmanagement.common.ApiResponse;
import com.stmanagement.dto.PurchaseOrderDTO;
import com.stmanagement.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController @RequestMapping("/api/purchase-order") @RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<PurchaseOrderDTO> r = purchaseOrderService.findAll(customerId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(r));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(purchaseOrderService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PurchaseOrderDTO dto) {
        return ResponseEntity.status(201).body(ApiResponse.success(purchaseOrderService.create(dto), "注文書を登録しました"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody PurchaseOrderDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(purchaseOrderService.update(id, dto), "注文書を更新しました"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        purchaseOrderService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "注文書を削除しました"));
    }
}
