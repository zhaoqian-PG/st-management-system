package com.stmanagement.repository;

import com.stmanagement.model.OrderDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDocumentRepository extends JpaRepository<OrderDocument, Long> {
    List<OrderDocument> findByInvoiceId(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}
