package com.stmanagement.controller;

import com.stmanagement.common.ApiResponse;
import com.stmanagement.dto.AttendanceDTO;
import com.stmanagement.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 10);
        Page<AttendanceDTO> r = attendanceService.findAll(year, month, employeeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(r));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AttendanceDTO dto) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success(attendanceService.create(dto), "勤務記録を登録しました"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody AttendanceDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.update(id, dto), "勤務記録を更新しました"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        attendanceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "勤務記録を削除しました"));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(
            @RequestParam Integer year, @RequestParam Integer month,
            @RequestParam Long employeeId) {
        int count = attendanceService.generateMonth(year, month, employeeId);
        return ResponseEntity.ok(ApiResponse.success(count, count + "件の勤務記録を生成しました"));
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportCsv(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long employeeId) {
        String csv = attendanceService.exportCsv(year, month, employeeId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance.csv")
                .body(csv);
    }
}
