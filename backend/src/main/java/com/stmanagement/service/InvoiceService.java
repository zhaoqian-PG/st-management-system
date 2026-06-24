package com.stmanagement.service;

import com.stmanagement.dto.InvoiceDTO;
import com.stmanagement.dto.InvoiceDetailDTO;
import com.stmanagement.dto.OrderDocumentDTO;
import com.stmanagement.model.Customer;
import com.stmanagement.model.Employee;
import com.stmanagement.model.Invoice;
import com.stmanagement.model.InvoiceDetail;
import com.stmanagement.model.OrderDocument;
import com.stmanagement.repository.BankAccountRepository;
import com.stmanagement.repository.CustomerRepository;
import com.stmanagement.repository.EmployeeRepository;
import com.stmanagement.repository.InvoiceDetailRepository;
import com.stmanagement.repository.InvoiceRepository;
import com.stmanagement.repository.OrderDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository detailRepository;
    private final OrderDocumentRepository orderDocumentRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final BankAccountRepository bankAccountRepository;
    private final Path uploadDir = Paths.get("uploads");

    public Page<InvoiceDTO> findAll(Integer year, Integer month, Long customerId, int page, int size) {
        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (year != null) predicates.add(cb.equal(root.get("year"), year));
            if (month != null) predicates.add(cb.equal(root.get("month"), month));
            if (customerId != null) predicates.add(cb.equal(root.get("customerId"), customerId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return invoiceRepository.findAll(spec, PageRequest.of(page, size, Sort.by("invoiceNumber").ascending()))
                .map(this::toDTO);
    }

    public InvoiceDTO findById(Long id) {
        Invoice inv = invoiceRepository.findById(id).orElseThrow(() -> new RuntimeException("請求書が見つかりません: " + id));
        InvoiceDTO dto = toDTO(inv);
        dto.setDetails(detailRepository.findByInvoiceId(id).stream().map(this::toDetailDTO).collect(Collectors.toList()));
        dto.setDocuments(orderDocumentRepository.findByInvoiceId(id).stream().map(this::toDocDTO).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public InvoiceDTO create(InvoiceDTO dto) {
        Invoice inv = toEntity(dto);
        inv.setInvoiceNumber(null); // DB auto-generates
        inv.setStatus("下書き");
        inv = invoiceRepository.save(inv);
        saveDetails(inv.getId(), dto.getDetails());
        return toDTO(inv);
    }

    @Transactional
    public InvoiceDTO update(Long id, InvoiceDTO dto) {
        Invoice inv = invoiceRepository.findById(id).orElseThrow(() -> new RuntimeException("請求書が見つかりません: " + id));
        inv.setCustomerId(dto.getCustomerId());
        inv.setYear(dto.getYear()); inv.setMonth(dto.getMonth());
        inv.setInvoiceDate(dto.getInvoiceDate()); inv.setDueDate(dto.getDueDate());
        inv.setAmount(dto.getAmount());
        inv.setTaxRate(dto.getTaxRate() != null ? dto.getTaxRate() : 10.0);
        double tax = Math.round(dto.getAmount() * (dto.getTaxRate() != null ? dto.getTaxRate() : 10.0)) / 100.0;
        inv.setTaxAmount(tax);
        inv.setTotalWithTax(dto.getAmount() + tax);
        inv.setSubject(dto.getSubject());
        inv.setStatus(dto.getStatus()); inv.setRemark(dto.getRemark());
        inv = invoiceRepository.save(inv);
        if (dto.getDetails() != null) { detailRepository.deleteByInvoiceId(id); saveDetails(id, dto.getDetails()); }
        return toDTO(inv);
    }

    @Transactional
    public void delete(Long id) {
        if (!invoiceRepository.existsById(id)) throw new RuntimeException("請求書が見つかりません: " + id);
        orderDocumentRepository.deleteByInvoiceId(id);
        invoiceRepository.deleteById(id);
    }

    @Transactional
    public OrderDocumentDTO uploadDocument(Long invoiceId, MultipartFile file) throws IOException {
        if (!invoiceRepository.existsById(invoiceId)) throw new RuntimeException("請求書が見つかりません: " + invoiceId);
        Files.createDirectories(uploadDir);
        String name = "inv_" + invoiceId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path target = uploadDir.resolve(name);
        Files.copy(file.getInputStream(), target);
        OrderDocument doc = OrderDocument.builder().invoiceId(invoiceId).fileName(file.getOriginalFilename())
                .filePath(target.toString()).fileSize(file.getSize()).build();
        return toDocDTO(orderDocumentRepository.save(doc));
    }

    @Transactional
    public void deleteDocument(Long docId) {
        OrderDocument doc = orderDocumentRepository.findById(docId).orElseThrow(() -> new RuntimeException("ファイルが見つかりません"));
        try { Files.deleteIfExists(Paths.get(doc.getFilePath())); } catch (IOException ignored) {}
        orderDocumentRepository.delete(doc);
    }

    private InvoiceDTO toDTO(Invoice inv) {
        String name = customerRepository.findById(inv.getCustomerId()).map(Customer::getCompanyName).orElse("");
        return InvoiceDTO.builder().id(inv.getId()).invoiceNumber(inv.getInvoiceNumber())
                .customerId(inv.getCustomerId()).customerName(name)
                .year(inv.getYear()).month(inv.getMonth()).invoiceDate(inv.getInvoiceDate()).dueDate(inv.getDueDate())
                .amount(inv.getAmount()).taxRate(inv.getTaxRate()).taxAmount(inv.getTaxAmount())
                .totalWithTax(inv.getTotalWithTax()).subject(inv.getSubject())
                .status(inv.getStatus()).remark(inv.getRemark()).build();
    }

    private Invoice toEntity(InvoiceDTO dto) {
        double amount = dto.getAmount() != null ? dto.getAmount() : 0;
        double rate = dto.getTaxRate() != null ? dto.getTaxRate() : 10.0;
        double tax = Math.round(amount * rate) / 100.0;
        return Invoice.builder().invoiceNumber(dto.getInvoiceNumber()).customerId(dto.getCustomerId())
                .year(dto.getYear()).month(dto.getMonth())
                .invoiceDate(dto.getInvoiceDate()).dueDate(dto.getDueDate())
                .amount(amount).taxRate(rate).taxAmount(tax).totalWithTax(amount + tax)
                .subject(dto.getSubject()).remark(dto.getRemark()).build();
    }

    public Resource getDocumentFile(Long docId) {
        OrderDocument doc = orderDocumentRepository.findById(docId).orElseThrow(() -> new RuntimeException("ファイルが見つかりません"));
        Resource r = new FileSystemResource(Paths.get(doc.getFilePath()));
        if (!r.exists()) throw new RuntimeException("ファイルが存在しません");
        return r;
    }

    public String exportInvoiceCsv(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId).orElseThrow(() -> new RuntimeException("請求書が見つかりません"));
        Customer cust = customerRepository.findById(inv.getCustomerId()).orElse(null);
        String customerName = cust != null ? cust.getCompanyName() : "";
        String customerAddr = cust != null && cust.getAddress() != null ? cust.getAddress() : "";
        String customerTel = cust != null && cust.getPhone() != null ? cust.getPhone() : "";
        String contactName = cust != null && cust.getContactName() != null ? cust.getContactName() : "";

        // Get primary bank account for this customer
        String bankInfo = "";
        List<com.stmanagement.model.BankAccount> bankAccounts = bankAccountRepository.findByCustomerId(inv.getCustomerId());
        com.stmanagement.model.BankAccount defaultBank = bankAccounts.stream().filter(b -> b.getIsDefault() != null && b.getIsDefault()).findFirst()
                .orElse(bankAccounts.isEmpty() ? null : bankAccounts.get(0));
        if (defaultBank != null) {
            bankInfo = defaultBank.getBankName() + " " + defaultBank.getBranchCode() + "支店 "
                    + defaultBank.getAccountType() + " " + defaultBank.getAccountNumber() + " "
                    + defaultBank.getAccountHolder();
        }

        List<InvoiceDetail> details = detailRepository.findByInvoiceId(invoiceId);

        StringBuilder sb = new StringBuilder();
        sb.append("　　　　　　　請　求　書\n\n");
        sb.append(inv.getYear()).append("年").append(inv.getMonth()).append("月").append("分\n\n");
        if (inv.getInvoiceDate() != null) sb.append("請求日: ").append(inv.getInvoiceDate()).append("\n");
        if (inv.getDueDate() != null) sb.append("支払期限: ").append(inv.getDueDate()).append("\n\n");

        sb.append("【請求先】\n");
        sb.append(customerName).append("\n");
        if (!customerAddr.isEmpty()) sb.append(customerAddr).append("\n");
        if (!contactName.isEmpty()) sb.append("担当: ").append(contactName).append("\n");
        if (!customerTel.isEmpty()) sb.append("TEL: ").append(customerTel).append("\n\n");

        sb.append("請求書番号: ").append(inv.getInvoiceNumber()).append("\n");
        if (inv.getSubject() != null && !inv.getSubject().isEmpty())
            sb.append("件名: ").append(inv.getSubject()).append("\n\n");

        sb.append("【明細】\n");
        sb.append("担当者,項目,数量,単価,金額,備考\n");
        double sub = 0;
        for (InvoiceDetail d : details) {
            String name = d.getEmployeeName() != null ? d.getEmployeeName() : "";
            sb.append(name).append(",").append(d.getDescription() != null ? d.getDescription() : "").append(",");
            sb.append(d.getQuantity()).append(",").append(d.getUnitPrice()).append(",");
            sb.append(d.getAmount()).append(",").append(d.getIsOvertime() ? "残業" : "").append("\n");
            sub += d.getAmount() != null ? d.getAmount() : 0;
        }

        sb.append("\n小計,,").append(String.format("%.0f", sub)).append("\n");
        double rate = inv.getTaxRate() != null ? inv.getTaxRate() : 10;
        double tax = inv.getTaxAmount() != null ? inv.getTaxAmount() : Math.round(sub * rate) / 100.0;
        double total = inv.getTotalWithTax() != null ? inv.getTotalWithTax() : sub + tax;
        sb.append("消費税(").append(String.format("%.0f", rate)).append("%),,").append(String.format("%.0f", tax)).append("\n");
        sb.append("税込合計,,").append(String.format("%.0f", total)).append("\n\n");

        if (!bankInfo.isEmpty()) {
            sb.append("【振込先口座】\n");
            sb.append(bankInfo).append("\n");
        }

        sb.append("\n備考: ").append(inv.getRemark() != null ? inv.getRemark() : "");
        return sb.toString();
    }

    public String getDocumentFileName(Long docId) {
        return orderDocumentRepository.findById(docId).map(OrderDocument::getFileName).orElse("download");
    }

    private void saveDetails(Long invoiceId, List<InvoiceDetailDTO> details) {
        if (details == null) return;
        for (InvoiceDetailDTO d : details) {
            double amount = (d.getQuantity() != null ? d.getQuantity() : 0) * (d.getUnitPrice() != null ? d.getUnitPrice() : 0);
            detailRepository.save(InvoiceDetail.builder().invoiceId(invoiceId).employeeId(d.getEmployeeId())
                    .employeeName(d.getEmployeeName()).description(d.getDescription())
                    .quantity(d.getQuantity()).unitPrice(d.getUnitPrice())
                    .amount(amount).isOvertime(d.getIsOvertime() != null ? d.getIsOvertime() : false).build());
        }
    }

    private InvoiceDetailDTO toDetailDTO(InvoiceDetail d) {
        String name = d.getEmployeeName() != null ? d.getEmployeeName() : (d.getEmployeeId() != null ? employeeRepository.findById(d.getEmployeeId()).map(Employee::getName).orElse("") : "");
        return InvoiceDetailDTO.builder().id(d.getId()).invoiceId(d.getInvoiceId()).employeeId(d.getEmployeeId())
                .employeeName(name).description(d.getDescription()).quantity(d.getQuantity())
                .unitPrice(d.getUnitPrice()).amount(d.getAmount()).isOvertime(d.getIsOvertime()).build();
    }

    private OrderDocumentDTO toDocDTO(OrderDocument doc) {
        return OrderDocumentDTO.builder().id(doc.getId()).invoiceId(doc.getInvoiceId())
                .fileName(doc.getFileName()).filePath(doc.getFilePath()).fileSize(doc.getFileSize()).build();
    }
}
