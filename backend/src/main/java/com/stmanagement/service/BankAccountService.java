package com.stmanagement.service;

import com.stmanagement.dto.BankAccountDTO;
import com.stmanagement.model.BankAccount;
import com.stmanagement.repository.BankAccountRepository;
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
    private final EntityManager entityManager;

    /**
     * Get all bank accounts with optional customer filter and pagination.
     */
    public Page<BankAccountDTO> findAll(Long customerId, int page, int size) {
        Specification<BankAccount> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageable = PageRequest.of(page, size, Sort.by("torihikiNo").ascending());
        return bankAccountRepository.findAll(spec, pageable).map(this::toDTO);
    }

    /**
     * Get bank account by ID.
     */
    public BankAccountDTO findById(Long id) {
        BankAccount entity = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("銀行口座が見つかりません: " + id));
        return toDTO(entity);
    }

    /**
     * Get bank accounts for a specific customer.
     */
    public List<BankAccountDTO> findByCustomerId(Long customerId) {
        return bankAccountRepository.findByCustomerId(customerId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Create a new bank account.
     */
    @Transactional
    public BankAccountDTO create(BankAccountDTO dto) {
        BankAccount entity = toEntity(dto);
        entity = bankAccountRepository.save(entity);
        // Refresh from DB to get auto-generated torihiki_no (insertable=false)
        entityManager.flush();
        entityManager.refresh(entity);
        return toDTO(entity);
    }

    /**
     * Update an existing bank account.
     */
    @Transactional
    public BankAccountDTO update(Long id, BankAccountDTO dto) {
        BankAccount entity = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("銀行口座が見つかりません: " + id));
        entity.setCustomerId(dto.getCustomerId());
        entity.setBranchCode(dto.getBranchCode());
        entity.setBankName(dto.getBankName());
        entity.setAccountType(dto.getAccountType());
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setAccountHolder(dto.getAccountHolder());
        entity = bankAccountRepository.save(entity);
        return toDTO(entity);
    }

    /**
     * Delete a bank account.
     */
    @Transactional
    public void delete(Long id) {
        if (!bankAccountRepository.existsById(id)) {
            throw new RuntimeException("銀行口座が見つかりません: " + id);
        }
        bankAccountRepository.deleteById(id);
    }

    /**
     * Bind a bank account to a customer.
     */
    @Transactional
    public void bindToCustomer(Long bankAccountId, Long customerId) {
        BankAccount entity = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("銀行口座が見つかりません: " + bankAccountId));
        entity.setCustomerId(customerId);
        bankAccountRepository.save(entity);
    }

    /**
     * Unbind a bank account from its customer.
     */
    @Transactional
    public void unbindFromCustomer(Long bankAccountId) {
        BankAccount entity = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("銀行口座が見つかりません: " + bankAccountId));
        entity.setCustomerId(null);
        bankAccountRepository.save(entity);
    }

    private BankAccountDTO toDTO(BankAccount entity) {
        return BankAccountDTO.builder()
                .id(entity.getId())
                .torihikiNo(entity.getTorihikiNo())
                .customerId(entity.getCustomerId())
                .branchCode(entity.getBranchCode())
                .bankName(entity.getBankName())
                .accountType(entity.getAccountType())
                .accountNumber(entity.getAccountNumber())
                .accountHolder(entity.getAccountHolder())
                .build();
    }

    private BankAccount toEntity(BankAccountDTO dto) {
        return BankAccount.builder()
                .customerId(dto.getCustomerId())
                .branchCode(dto.getBranchCode())
                .bankName(dto.getBankName())
                .accountType(dto.getAccountType())
                .accountNumber(dto.getAccountNumber())
                .accountHolder(dto.getAccountHolder())
                .build();
    }
}
