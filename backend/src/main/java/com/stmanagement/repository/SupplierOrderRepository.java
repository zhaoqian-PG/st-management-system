package com.stmanagement.repository;
import com.stmanagement.model.SupplierOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long>, JpaSpecificationExecutor<SupplierOrder> {}
