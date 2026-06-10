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
        PurchaseOrder po = toEntity(dto);
        po.setOrderNumber(null); // DB auto-generates
        po.setStatus("下書き");
        po = orderRepository.save(po);
        if (dto.getDetails() != null) saveDetails(po.getId(), dto.getDetails());
        return toDTO(po);
    }

    @Transactional
    public PurchaseOrderDTO update(Long id, PurchaseOrderDTO dto) {
        PurchaseOrder po = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("注文書が見つかりません: " + id));
        po.setCustomerId(dto.getCustomerId());
        po.setOrderDate(dto.getOrderDate()); po.setDeliveryDate(dto.getDeliveryDate());
        po.setRecipientDept(dto.getRecipientDept()); po.setRecipientName(dto.getRecipientName());
        po.setRecipientAddr(dto.getRecipientAddr()); po.setRecipientTel(dto.getRecipientTel());
        po.setIssuerName(dto.getIssuerName()); po.setIssuerDept(dto.getIssuerDept()); po.setIssuerTel(dto.getIssuerTel());
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

    public String exportOrderCsv(Long orderId) {
        PurchaseOrder po = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("注文書が見つかりません"));
        Customer cust = customerRepository.findById(po.getCustomerId()).orElse(null);
        String customerName = cust != null ? cust.getCompanyName() : "";
        String customerAddr = cust != null && cust.getAddress() != null ? cust.getAddress() : "";
        List<PurchaseOrderDetail> details = detailRepository.findByOrderId(orderId);

        StringBuilder sb = new StringBuilder();
        sb.append("　　　　　　　注　文　書\n\n");
        sb.append("注文番号: ").append(po.getOrderNumber()).append("\n");
        sb.append("注文日: ").append(po.getOrderDate()).append("\n");
        if (po.getDeliveryDate() != null) sb.append("納品期限: ").append(po.getDeliveryDate()).append("\n");
        sb.append("\n【発注先】\n");
        sb.append(customerName).append("\n");
        if (po.getRecipientDept() != null) sb.append(po.getRecipientDept()).append("　");
        if (po.getRecipientName() != null) sb.append(po.getRecipientName()).append(" 様\n");
        if (customerAddr != null && !customerAddr.isEmpty()) sb.append(customerAddr).append("\n");
        if (po.getRecipientTel() != null) sb.append("TEL: ").append(po.getRecipientTel()).append("\n\n");
        if (po.getSubject() != null && !po.getSubject().isEmpty())
            sb.append("件名: ").append(po.getSubject()).append("\n\n");

        sb.append("【明細】\n担当者,品名,数量,単価,金額\n");
        double sub = 0;
        for (PurchaseOrderDetail d : details) {
            sb.append(d.getEmployeeName() != null ? d.getEmployeeName() : "").append(",");
            sb.append(d.getItemName()).append(",").append(d.getQuantity()).append(",");
            sb.append(d.getUnitPrice()).append(",").append(d.getAmount()).append("\n");
            sub += d.getAmount() != null ? d.getAmount() : 0;
        }
        sb.append("\n小計,,").append(String.format("%.0f", sub)).append("\n");
        double rate = po.getTaxRate() != null ? po.getTaxRate() : 10;
        double tax = po.getTaxAmount() != null ? po.getTaxAmount() : Math.round(sub * rate) / 100.0;
        double total = po.getTotalWithTax() != null ? po.getTotalWithTax() : sub + tax;
        sb.append("消費税(").append(String.format("%.0f", rate)).append("%),,").append(String.format("%.0f", tax)).append("\n");
        sb.append("合計,,").append(String.format("%.0f", total)).append("\n\n");
        sb.append("備考: ").append(po.getRemark() != null ? po.getRemark() : "").append("\n");
        return sb.toString();
    }

    private void saveDetails(Long orderId, List<PurchaseOrderDetailDTO> details) {
        if (details == null) return;
        for (PurchaseOrderDetailDTO d : details) {
            detailRepository.save(PurchaseOrderDetail.builder().orderId(orderId)
                    .employeeName(d.getEmployeeName()).itemName(d.getItemName())
                    .quantity(d.getQuantity()).unitPrice(d.getUnitPrice())
                    .amount((d.getQuantity() != null ? d.getQuantity() : 0) * (d.getUnitPrice() != null ? d.getUnitPrice() : 0))
                    .remark(d.getRemark()).build());
        }
    }

    private PurchaseOrderDTO toDTO(PurchaseOrder po) {
        String name = customerRepository.findById(po.getCustomerId()).map(Customer::getCompanyName).orElse("");
        return PurchaseOrderDTO.builder().id(po.getId()).orderNumber(po.getOrderNumber()).customerId(po.getCustomerId())
                .customerName(name).orderDate(po.getOrderDate()).deliveryDate(po.getDeliveryDate())
                .recipientDept(po.getRecipientDept()).recipientName(po.getRecipientName())
                .recipientAddr(po.getRecipientAddr()).recipientTel(po.getRecipientTel())
                .issuerName(po.getIssuerName()).issuerDept(po.getIssuerDept()).issuerTel(po.getIssuerTel())
                .subject(po.getSubject()).amount(po.getAmount()).taxRate(po.getTaxRate()).taxAmount(po.getTaxAmount())
                .totalWithTax(po.getTotalWithTax()).status(po.getStatus()).remark(po.getRemark()).build();
    }

    private PurchaseOrder toEntity(PurchaseOrderDTO dto) {
        double amount = dto.getAmount() != null ? dto.getAmount() : 0;
        double rate = dto.getTaxRate() != null ? dto.getTaxRate() : 10;
        double tax = Math.round(amount * rate) / 100.0;
        return PurchaseOrder.builder().orderNumber(dto.getOrderNumber()).customerId(dto.getCustomerId())
                .orderDate(dto.getOrderDate()).deliveryDate(dto.getDeliveryDate())
                .recipientDept(dto.getRecipientDept()).recipientName(dto.getRecipientName())
                .recipientAddr(dto.getRecipientAddr()).recipientTel(dto.getRecipientTel()).subject(dto.getSubject())
                .amount(amount).taxRate(rate).taxAmount(tax).totalWithTax(amount + tax).remark(dto.getRemark()).build();
    }

    private PurchaseOrderDetailDTO toDetailDTO(PurchaseOrderDetail d) {
        return PurchaseOrderDetailDTO.builder().id(d.getId()).orderId(d.getOrderId())
                .employeeName(d.getEmployeeName()).itemName(d.getItemName())
                .quantity(d.getQuantity()).unitPrice(d.getUnitPrice())
                .amount(d.getAmount()).remark(d.getRemark()).build();
    }
}
