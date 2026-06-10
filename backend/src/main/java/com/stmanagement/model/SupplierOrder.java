package com.stmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "supplier_order")
public class SupplierOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_number", length = 50, nullable = false, unique = true) private String orderNumber;
    @Column(name = "supplier_name", length = 200, nullable = false) private String supplierName;
    @Column(name = "order_date", nullable = false) private LocalDate orderDate;
    @Column(name = "delivery_date") private LocalDate deliveryDate;
    @Column(length = 500) private String subject;
    @Column(nullable = false) private Double amount;
    @Column(name = "tax_rate") private Double taxRate;
    @Column(name = "tax_amount") private Double taxAmount;
    @Column(name = "total_with_tax") private Double totalWithTax;
    @Column(length = 20, nullable = false) private String status;
    @Column(columnDefinition = "TEXT") private String remark;
    @Column(name = "create_time", nullable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", nullable = false) private LocalDateTime updateTime;
    @PrePersist protected void onCreate() { createTime = LocalDateTime.now(); updateTime = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updateTime = LocalDateTime.now(); }
}
