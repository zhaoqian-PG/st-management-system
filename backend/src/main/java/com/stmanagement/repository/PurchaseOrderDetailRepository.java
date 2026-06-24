package com.stmanagement.repository;

import com.stmanagement.model.PurchaseOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseOrderDetailRepository extends JpaRepository<PurchaseOrderDetail, Long> {
    List<PurchaseOrderDetail> findByOrderId(Long orderId);
    void deleteByOrderId(Long orderId);
}
