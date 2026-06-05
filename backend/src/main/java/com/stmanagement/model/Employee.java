package com.stmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", length = 20, nullable = false, unique = true,
            insertable = false, updatable = false)
    private String employeeCode;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "japan_address", length = 500)
    private String japanAddress;

    @Column(name = "china_address", length = 500)
    private String chinaAddress;

    @Column(name = "china_phone", length = 20)
    private String chinaPhone;

    @Column(name = "china_emergency_contact", length = 100)
    private String chinaEmergencyContact;

    @Column(name = "torihiki_no", length = 500)
    private String torihikiNo;

    @Column(length = 10, nullable = false)
    private String status;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String position;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

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
