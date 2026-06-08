package com.stmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDTO {
    private Long id;
    @NotNull private Long employeeId;
    private String employeeName;
    @NotNull private LocalDate workDate;
    @NotNull private Double workHours;
    private Double overtimeHours;
    private String clockIn;
    private String clockOut;
    private Double totalHours;
    private String status;
    private String remark;
}
