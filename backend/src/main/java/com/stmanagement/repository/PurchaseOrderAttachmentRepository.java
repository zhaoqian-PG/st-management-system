package com.stmanagement.repository;
import com.stmanagement.model.PurchaseOrderAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseOrderAttachmentRepository extends JpaRepository<PurchaseOrderAttachment, Long> {
    List<PurchaseOrderAttachment> findByOrderId(Long orderId);
    void deleteByOrderId(Long orderId);
}
