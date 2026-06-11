package com.stmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "purchase_order_attachment")
public class PurchaseOrderAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "file_name", length = 255, nullable = false) private String fileName;
    @Column(name = "file_path", length = 500, nullable = false) private String filePath;
    @Column(name = "file_size", nullable = false) private Long fileSize;
    @Column(name = "create_time", nullable = false, updatable = false) private LocalDateTime createTime;
    @PrePersist protected void onCreate() { createTime = LocalDateTime.now(); }
}
