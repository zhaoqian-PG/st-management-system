package com.stmanagement.controller;

import com.stmanagement.common.ApiResponse;
import com.stmanagement.dto.BankAccountDTO;
import com.stmanagement.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    /**
     * GET /api/bank-accounts — 銀行口座一覧取得
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 10);
        Page<BankAccountDTO> result = bankAccountService.findAll(category, customerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/torihiki-nos/{category}")
    public ResponseEntity<?> torihikiNos(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.getExistingTorihikiNos(category)));
    }

    /**
     * GET /api/bank-accounts/{id} — 銀行口座詳細取得
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        BankAccountDTO dto = bankAccountService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * GET /api/bank-accounts/customer/{customerId} — 顧客別銀行口座一覧
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getByCustomerId(@PathVariable Long customerId) {
        List<BankAccountDTO> list = bankAccountService.findByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getByEmployeeId(@PathVariable Long employeeId) {
        List<BankAccountDTO> list = bankAccountService.findByEmployeeId(employeeId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * POST /api/bank-accounts — 銀行口座新規登録
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody BankAccountDTO dto) {
        BankAccountDTO created = bankAccountService.create(dto);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "銀行口座を登録しました"));
    }

    /**
     * PUT /api/bank-accounts/{id} — 銀行口座更新
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id, @Valid @RequestBody BankAccountDTO dto) {
        BankAccountDTO updated = bankAccountService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "銀行口座を更新しました"));
    }

    /**
     * DELETE /api/bank-accounts/{id} — 銀行口座削除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        bankAccountService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "銀行口座を削除しました"));
    }

    /**
     * PUT /api/bank-accounts/{id}/bind/{customerId} — 顧客紐付け
     */
    @PutMapping("/{id}/bind/{customerId}")
    public ResponseEntity<?> bindToCustomer(
            @PathVariable Long id, @PathVariable Long customerId) {
        bankAccountService.bindToCustomer(id, customerId);
        return ResponseEntity.ok(ApiResponse.success(null, "銀行口座を顧客に紐付けました"));
    }

    /**
     * PUT /api/bank-accounts/{id}/unbind — 紐付け解除
     */
    @PutMapping("/{id}/unbind")
    public ResponseEntity<?> unbindFromCustomer(@PathVariable Long id) {
        bankAccountService.unbindFromCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(null, "銀行口座の紐付けを解除しました"));
    }

    @PutMapping("/{id}/bind-employee/{employeeId}")
    public ResponseEntity<?> bindToEmployee(@PathVariable Long id, @PathVariable Long employeeId) {
        bankAccountService.bindToEmployee(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(null, "銀行口座を社員に紐付けました"));
    }

    @PutMapping("/{id}/unbind-employee")
    public ResponseEntity<?> unbindFromEmployee(@PathVariable Long id) {
        bankAccountService.unbindFromEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(null, "社員紐付けを解除しました"));
    }

    @PutMapping("/{id}/set-default-employee/{employeeId}")
    public ResponseEntity<?> setDefaultForEmployee(@PathVariable Long id, @PathVariable Long employeeId) {
        bankAccountService.setDefaultForEmployee(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(null, "既定口座を設定しました"));
    }

    @PutMapping("/{id}/set-default-customer/{customerId}")
    public ResponseEntity<?> setDefaultForCustomer(@PathVariable Long id, @PathVariable Long customerId) {
        bankAccountService.setDefaultForCustomer(id, customerId);
        return ResponseEntity.ok(ApiResponse.success(null, "既定口座を設定しました"));
    }
}
