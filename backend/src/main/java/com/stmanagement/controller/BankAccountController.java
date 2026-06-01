package com.stmanagement.controller;

import com.stmanagement.dto.BankAccountDTO;
import com.stmanagement.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    /**
     * GET /api/bank-accounts
     * List all bank accounts, optionally filtered by customerId.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 10);
        Page<BankAccountDTO> result = bankAccountService.findAll(customerId, page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("message", "success");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/bank-accounts/{id}
     * Get bank account detail with linked customer info.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        BankAccountDTO dto = bankAccountService.findById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("data", dto);
        response.put("message", "success");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/bank-accounts/customer/{customerId}
     * Get all bank accounts for a specific customer.
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getByCustomerId(@PathVariable Long customerId) {
        List<BankAccountDTO> list = bankAccountService.findByCustomerId(customerId);
        Map<String, Object> response = new HashMap<>();
        response.put("data", list);
        response.put("message", "success");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/bank-accounts
     * Create a new bank account.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody BankAccountDTO dto) {
        BankAccountDTO created = bankAccountService.create(dto);
        Map<String, Object> response = new HashMap<>();
        response.put("data", created);
        response.put("message", "銀行口座を登録しました");
        return ResponseEntity.status(201).body(response);
    }

    /**
     * PUT /api/bank-accounts/{id}
     * Update an existing bank account.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id, @Valid @RequestBody BankAccountDTO dto) {
        BankAccountDTO updated = bankAccountService.update(id, dto);
        Map<String, Object> response = new HashMap<>();
        response.put("data", updated);
        response.put("message", "銀行口座を更新しました");
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/bank-accounts/{id}
     * Delete a bank account.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        bankAccountService.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("data", null);
        response.put("message", "銀行口座を削除しました");
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/bank-accounts/{id}/bind/{customerId}
     * Bind a bank account to a customer.
     */
    @PutMapping("/{id}/bind/{customerId}")
    public ResponseEntity<Map<String, Object>> bindToCustomer(
            @PathVariable Long id, @PathVariable Long customerId) {
        bankAccountService.bindToCustomer(id, customerId);
        Map<String, Object> response = new HashMap<>();
        response.put("data", null);
        response.put("message", "銀行口座を顧客に紐付けました");
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/bank-accounts/{id}/unbind
     * Unbind a bank account from its customer.
     */
    @PutMapping("/{id}/unbind")
    public ResponseEntity<Map<String, Object>> unbindFromCustomer(@PathVariable Long id) {
        bankAccountService.unbindFromCustomer(id);
        Map<String, Object> response = new HashMap<>();
        response.put("data", null);
        response.put("message", "銀行口座の紐付けを解除しました");
        return ResponseEntity.ok(response);
    }
}
