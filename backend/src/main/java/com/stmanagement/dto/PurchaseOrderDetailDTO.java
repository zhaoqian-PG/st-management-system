package com.stmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrderDetailDTO {
    private Long id;
    private Long orderId;
    private String employeeName;
    private String itemName;
    private Double quantity;
    private Double unitPrice;
    private Double amount;
    private String remark;
}
