package com.stmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "work_hours", columnDefinition = "DECIMAL(4,1) DEFAULT 0", nullable = false)
    private BigDecimal workHours;

    @Column(name = "overtime_hours", columnDefinition = "DECIMAL(4,1) DEFAULT 0", nullable = false)
    private BigDecimal overtimeHours;

    @Column(name = "clock_in")
    private java.time.LocalTime clockIn;

    @Column(name = "clock_out")
    private java.time.LocalTime clockOut;

    @Column(name = "total_hours", columnDefinition = "DECIMAL(4,1)")
    private BigDecimal totalHours;

    @Column(name = "work_type", length = 20, nullable = false)
    private String workType;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(length = 500)
    private String remark;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
