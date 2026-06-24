package com.stmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
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
    @NotNull private BigDecimal workHours;
    private BigDecimal overtimeHours;
    private String clockIn;
    private String clockOut;
    private BigDecimal totalHours;
    private String workType;
    private String status;
    private String remark;
}
