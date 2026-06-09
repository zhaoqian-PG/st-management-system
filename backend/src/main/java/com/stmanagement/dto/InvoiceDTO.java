package com.stmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {
    private Long id;
    @NotBlank private String invoiceNumber;
    @NotNull private Long customerId;
    private String customerName;
    @NotNull private Integer year;
    @NotNull private Integer month;
    @NotNull private Double amount;
    private String status;
    private String remark;
    private List<OrderDocumentDTO> documents;
}
