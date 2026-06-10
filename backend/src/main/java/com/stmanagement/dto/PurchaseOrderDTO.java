package com.stmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrderDTO {
    private Long id;
    private String orderNumber;
    @NotNull private Long customerId;
    private String customerName;
    @NotNull private LocalDate orderDate;
    private LocalDate deliveryDate;
    private String recipientDept;
    private String recipientName;
    private String recipientAddr;
    private String recipientTel;
    private String issuerName;
    private String issuerDept;
    private String issuerTel;
    private String subject;
    private Double amount;
    private Double taxRate;
    private Double taxAmount;
    private Double totalWithTax;
    private String status;
    private String remark;
    private List<PurchaseOrderDetailDTO> details;
}
