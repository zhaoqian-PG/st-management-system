package com.stmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDetailDTO {
    private Long id;
    private Long invoiceId;
    private Long employeeId;
    private String employeeName;
    private String description;
    private Double quantity;
    private Double unitPrice;
    private Double amount;
    private Boolean isOvertime;
}
