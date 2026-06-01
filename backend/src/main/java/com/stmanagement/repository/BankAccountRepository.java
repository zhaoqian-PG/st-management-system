package com.stmanagement.repository;

import com.stmanagement.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long>,
        JpaSpecificationExecutor<BankAccount> {

    Optional<BankAccount> findByTorihikiNo(String torihikiNo);

    List<BankAccount> findByCustomerId(Long customerId);

    List<BankAccount> findByCustomerIdIsNull();

    boolean existsByTorihikiNo(String torihikiNo);
}
