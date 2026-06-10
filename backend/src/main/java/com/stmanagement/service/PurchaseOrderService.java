package com.stmanagement.service;

import com.stmanagement.dto.PurchaseOrderDTO;
import com.stmanagement.dto.PurchaseOrderDetailDTO;
import com.stmanagement.model.Customer;
import com.stmanagement.model.PurchaseOrder;
import com.stmanagement.model.PurchaseOrderDetail;
import com.stmanagement.repository.CustomerRepository;
import com.stmanagement.repository.PurchaseOrderDetailRepository;
import com.stmanagement.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderDetailRepository detailRepository;
    private final CustomerRepository customerRepository;

    public Page<PurchaseOrderDTO> findAll(Long customerId, String status, int page, int size) {
        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) predicates.add(cb.equal(root.get("customerId"), customerId));
            if (status != null && !status.isEmpty()) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return orderRepository.findAll(spec, PageRequest.of(page, size, Sort.by("orderDate").descending())).map(this::toDTO);
    }

    public PurchaseOrderDTO findById(Long id) {
        PurchaseOrder po = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("注文書が見つかりません: " + id));
        PurchaseOrderDTO dto = toDTO(po);
        dto.setDetails(detailRepository.findByOrderId(id).stream().map(this::toDetailDTO).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public PurchaseOrderDTO create(PurchaseOrderDTO dto) {
        if (orderRepository.existsByOrderNumber(dto.getOrderNumber()))
            throw new RuntimeException("注文番号 " + dto.getOrderNumber() + " は既に存在します");
        PurchaseOrder po = toEntity(dto);
        po.setStatus("下書き");
        po = orderRepository.save(po);
        if (dto.getDetails() != null) saveDetails(po.getId(), dto.getDetails());
        return toDTO(po);
    }

    @Transactional
    public PurchaseOrderDTO update(Long id, PurchaseOrderDTO dto) {
        PurchaseOrder po = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("注文書が見つかりません: " + id));
        if (!po.getOrderNumber().equals(dto.getOrderNumber()) && orderRepository.existsByOrderNumber(dto.getOrderNumber()))
            throw new RuntimeException("注文番号 " + dto.getOrderNumber() + " は既に存在します");
        po.setOrderNumber(dto.getOrderNumber()); po.setCustomerId(dto.getCustomerId());
        po.setOrderDate(dto.getOrderDate()); po.setDeliveryDate(dto.getDeliveryDate());
        po.setSubject(dto.getSubject()); po.setAmount(dto.getAmount());
        double rate = dto.getTaxRate() != null ? dto.getTaxRate() : 10;
        double tax = dto.getAmount() != null ? Math.round(dto.getAmount() * rate) / 100.0 : 0;
        po.setTaxRate(rate); po.setTaxAmount(tax); po.setTotalWithTax((dto.getAmount() != null ? dto.getAmount() : 0) + tax);
        po.setStatus(dto.getStatus()); po.setRemark(dto.getRemark());
        po = orderRepository.save(po);
        if (dto.getDetails() != null) { detailRepository.deleteByOrderId(id); saveDetails(id, dto.getDetails()); }
        return toDTO(po);
    }

    @Transactional public void delete(Long id) { detailRepository.deleteByOrderId(id); orderRepository.deleteById(id); }

    private void saveDetails(Long orderId, List<PurchaseOrderDetailDTO> details) {
        if (details == null) return;
        for (PurchaseOrderDetailDTO d : details) {
            detailRepository.save(PurchaseOrderDetail.builder().orderId(orderId).itemName(d.getItemName())
                    .quantity(d.getQuantity()).unitPrice(d.getUnitPrice())
                    .amount((d.getQuantity() != null ? d.getQuantity() : 0) * (d.getUnitPrice() != null ? d.getUnitPrice() : 0))
                    .remark(d.getRemark()).build());
        }
    }

    private PurchaseOrderDTO toDTO(PurchaseOrder po) {
        String name = customerRepository.findById(po.getCustomerId()).map(Customer::getCompanyName).orElse("");
        return PurchaseOrderDTO.builder().id(po.getId()).orderNumber(po.getOrderNumber()).customerId(po.getCustomerId())
                .customerName(name).orderDate(po.getOrderDate()).deliveryDate(po.getDeliveryDate())
                .subject(po.getSubject()).amount(po.getAmount()).taxRate(po.getTaxRate()).taxAmount(po.getTaxAmount())
                .totalWithTax(po.getTotalWithTax()).status(po.getStatus()).remark(po.getRemark()).build();
    }

    private PurchaseOrder toEntity(PurchaseOrderDTO dto) {
        double amount = dto.getAmount() != null ? dto.getAmount() : 0;
        double rate = dto.getTaxRate() != null ? dto.getTaxRate() : 10;
        double tax = Math.round(amount * rate) / 100.0;
        return PurchaseOrder.builder().orderNumber(dto.getOrderNumber()).customerId(dto.getCustomerId())
                .orderDate(dto.getOrderDate()).deliveryDate(dto.getDeliveryDate()).subject(dto.getSubject())
                .amount(amount).taxRate(rate).taxAmount(tax).totalWithTax(amount + tax).remark(dto.getRemark()).build();
    }

    private PurchaseOrderDetailDTO toDetailDTO(PurchaseOrderDetail d) {
        return PurchaseOrderDetailDTO.builder().id(d.getId()).orderId(d.getOrderId())
                .itemName(d.getItemName()).quantity(d.getQuantity()).unitPrice(d.getUnitPrice())
                .amount(d.getAmount()).remark(d.getRemark()).build();
    }
}
