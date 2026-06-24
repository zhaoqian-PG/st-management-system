package com.stmanagement.repository;

import com.stmanagement.model.InvoiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, Long> {
    List<InvoiceDetail> findByInvoiceId(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}
