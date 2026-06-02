package com.stmanagement.service;

import com.stmanagement.dto.BankAccountDTO;
import com.stmanagement.model.BankAccount;
import com.stmanagement.model.Customer;
import com.stmanagement.model.Employee;
import com.stmanagement.repository.BankAccountRepository;
import com.stmanagement.repository.CustomerRepository;
import com.stmanagement.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final EntityManager entityManager;

    public Page<BankAccountDTO> findAll(String category, Long customerId, int page, int size) {
        Specification<BankAccount> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null && !category.isEmpty())
                predicates.add(cb.equal(root.get("category"), category));
            if (customerId != null)
                predicates.add(cb.equal(root.get("customerId"), customerId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return bankAccountRepository.findAll(spec, PageRequest.of(page, size, Sort.by("torihikiNo").ascending()))
                .map(this::toDTO);
    }

    public BankAccountDTO findById(Long id) {
        return toDTO(bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("銀行口座が見つかりません: " + id)));
    }

    public List<BankAccountDTO> findByCustomerId(Long cid) {
        return bankAccountRepository.findByCustomerId(cid).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<BankAccountDTO> findByEmployeeId(Long eid) {
        return bankAccountRepository.findAll((Specification<BankAccount>) (root, q, cb) ->
                cb.equal(root.get("employeeId"), eid)).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public BankAccountDTO create(BankAccountDTO dto) {
        BankAccount e = toEntity(dto);
        e = bankAccountRepository.save(e);
        entityManager.flush();
        entityManager.refresh(e);
        return toDTO(e);
    }

    @Transactional
    public BankAccountDTO update(Long id, BankAccountDTO dto) {
        BankAccount e = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("銀行口座が見つかりません: " + id));
        e.setCategory(dto.getCategory());
        e.setCustomerId(dto.getCustomerId());
        e.setEmployeeId(dto.getEmployeeId());
        e.setBranchCode(dto.getBranchCode());
        e.setBankName(dto.getBankName());
        e.setAccountType(dto.getAccountType());
        e.setAccountNumber(dto.getAccountNumber());
        e.setAccountHolder(dto.getAccountHolder());
        return toDTO(bankAccountRepository.save(e));
    }

    @Transactional
    public void delete(Long id) {
        if (!bankAccountRepository.existsById(id))
            throw new RuntimeException("銀行口座が見つかりません: " + id);
        bankAccountRepository.deleteById(id);
    }

    @Transactional
    public void bindToCustomer(Long id, Long customerId) {
        BankAccount e = bankAccountRepository.findById(id).orElseThrow(() -> new RuntimeException("銀行口座が見つかりません"));
        e.setCategory("CUSTOMER");
        e.setCustomerId(customerId);
        e.setEmployeeId(null);
        bankAccountRepository.save(e);
        syncCustomerTorihikiNo(customerId);
    }

    @Transactional
    public void unbindFromCustomer(Long id) {
        BankAccount e = bankAccountRepository.findById(id).orElseThrow(() -> new RuntimeException("銀行口座が見つかりません"));
        Long cid = e.getCustomerId();
        e.setCustomerId(null);
        bankAccountRepository.save(e);
        if (cid != null) syncCustomerTorihikiNo(cid);
    }

    @Transactional
    public void bindToEmployee(Long id, Long employeeId) {
        BankAccount e = bankAccountRepository.findById(id).orElseThrow(() -> new RuntimeException("銀行口座が見つかりません"));
        e.setCategory("EMPLOYEE");
        e.setEmployeeId(employeeId);
        e.setCustomerId(null);
        bankAccountRepository.save(e);
        syncEmployeeTorihikiNo(employeeId);
    }

    @Transactional
    public void unbindFromEmployee(Long id) {
        BankAccount e = bankAccountRepository.findById(id).orElseThrow(() -> new RuntimeException("銀行口座が見つかりません"));
        Long eid = e.getEmployeeId();
        e.setEmployeeId(null);
        bankAccountRepository.save(e);
        if (eid != null) syncEmployeeTorihikiNo(eid);
    }

    private void syncCustomerTorihikiNo(Long cid) {
        Customer c = customerRepository.findById(cid).orElse(null);
        if (c == null) return;
        List<String> labels = bankAccountRepository.findByCustomerId(cid).stream()
                .map(ba -> ba.getBankName() + "（" + ba.getBranchCode() + "）")
                .collect(Collectors.toList());
        c.setTorihikiNo(labels.isEmpty() ? null : String.join("、", labels));
        customerRepository.save(c);
    }

    private void syncEmployeeTorihikiNo(Long eid) {
        Employee emp = employeeRepository.findById(eid).orElse(null);
        if (emp == null) return;
        List<String> labels = bankAccountRepository.findAll((Specification<BankAccount>) (root, q, cb) ->
                cb.equal(root.get("employeeId"), eid)).stream()
                .map(ba -> ba.getBankName() + "（" + ba.getBranchCode() + "）")
                .collect(Collectors.toList());
        emp.setTorihikiNo(labels.isEmpty() ? null : String.join("、", labels));
        employeeRepository.save(emp);
    }

    private BankAccountDTO toDTO(BankAccount e) {
        String cn = null, en = null;
        if (e.getCustomerId() != null) cn = customerRepository.findById(e.getCustomerId()).map(Customer::getCompanyName).orElse(null);
        if (e.getEmployeeId() != null) en = employeeRepository.findById(e.getEmployeeId()).map(Employee::getName).orElse(null);
        return BankAccountDTO.builder().id(e.getId()).torihikiNo(e.getTorihikiNo())
                .category(e.getCategory()).customerId(e.getCustomerId()).employeeId(e.getEmployeeId())
                .customerName(cn).employeeName(en)
                .branchCode(e.getBranchCode()).bankName(e.getBankName())
                .accountType(e.getAccountType()).accountNumber(e.getAccountNumber())
                .accountHolder(e.getAccountHolder()).build();
    }

    private BankAccount toEntity(BankAccountDTO dto) {
        return BankAccount.builder().category(dto.getCategory())
                .customerId(dto.getCustomerId()).employeeId(dto.getEmployeeId())
                .branchCode(dto.getBranchCode()).bankName(dto.getBankName())
                .accountType(dto.getAccountType()).accountNumber(dto.getAccountNumber())
                .accountHolder(dto.getAccountHolder()).build();
    }
}
