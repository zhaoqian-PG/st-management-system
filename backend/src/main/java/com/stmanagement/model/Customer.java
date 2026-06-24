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
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", length = 20, nullable = false, unique = true,
            insertable = false, updatable = false)
    private String customerCode;

    @Column(name = "company_name", length = 200, nullable = false)
    private String companyName;

    @Column(name = "president_name", length = 100)
    private String presidentName;

    @Column(length = 500)
    private String website;

    @Column(length = 500)
    private String address;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "sales_rep_name", length = 100)
    private String salesRepName;

    @Column(name = "sales_rep_phone", length = 20)
    private String salesRepPhone;

    @Column(name = "sales_rep_email", length = 255)
    private String salesRepEmail;

    @Column(name = "admin_rep_name", length = 100)
    private String adminRepName;

    @Column(name = "admin_rep_phone", length = 20)
    private String adminRepPhone;

    @Column(name = "admin_rep_email", length = 255)
    private String adminRepEmail;

    @Column(name = "torihiki_no", length = 500)
    private String torihikiNo;

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
