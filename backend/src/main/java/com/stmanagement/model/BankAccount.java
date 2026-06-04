package com.stmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bank_account")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "torihiki_no", length = 20, nullable = false, insertable = false)
    private String torihikiNo;

    @Column(name = "branch_no", length = 3, nullable = false)
    private String branchNo;

    @Column(length = 10)
    private String category;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "branch_code", length = 10)
    private String branchCode;

    @Column(name = "bank_name", length = 200, nullable = false)
    private String bankName;

    @Column(name = "account_type", length = 20, nullable = false)
    private String accountType;

    @Column(name = "account_number", length = 50, nullable = false)
    private String accountNumber;

    @Column(name = "account_holder", length = 100, nullable = false)
    private String accountHolder;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

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
