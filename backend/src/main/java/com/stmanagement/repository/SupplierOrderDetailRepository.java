package com.stmanagement.repository;
import com.stmanagement.model.SupplierOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SupplierOrderDetailRepository extends JpaRepository<SupplierOrderDetail, Long> {
    List<SupplierOrderDetail> findByOrderId(Long orderId);
    void deleteByOrderId(Long orderId);
}
