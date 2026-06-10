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

    @GetMapping(value = "/export/{id}", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportOrder(@PathVariable Long id) {
        String csv = purchaseOrderService.exportOrderCsv(id);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"purchase_order_" + id + ".csv\"")
                .body(result);
    }
}
