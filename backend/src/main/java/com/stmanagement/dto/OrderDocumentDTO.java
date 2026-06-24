package com.stmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDocumentDTO {
    private Long id;
    private Long invoiceId;
    private String fileName;
    private String filePath;
    private Long fileSize;
}
