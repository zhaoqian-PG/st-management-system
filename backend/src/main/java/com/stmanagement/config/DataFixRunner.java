package com.stmanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataFixRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.update("UPDATE bank_account SET branch_no = '001' WHERE branch_no IS NULL");
        jdbcTemplate.update("UPDATE bank_account SET category = 'CUSTOMER' WHERE category IS NULL AND customer_id IS NOT NULL");
        jdbcTemplate.update("UPDATE bank_account SET category = 'EMPLOYEE' WHERE category IS NULL AND employee_id IS NOT NULL");
        jdbcTemplate.update("UPDATE bank_account SET is_default = FALSE WHERE is_default IS NULL");
        // Sync employee.torihiki_no = bank_account.torihiki_no (group key)
        jdbcTemplate.update("UPDATE employee e SET torihiki_no = (" +
            "SELECT ba.torihiki_no FROM bank_account ba WHERE ba.employee_id = e.id AND ba.category = 'EMPLOYEE' FETCH FIRST 1 ROW ONLY" +
            ") WHERE torihiki_no IS NULL OR torihiki_no LIKE '%（%'");
        // Sync customer.torihiki_no = bank_account.torihiki_no (group key)
        jdbcTemplate.update("UPDATE customer c SET torihiki_no = (" +
            "SELECT ba.torihiki_no FROM bank_account ba WHERE ba.customer_id = c.id AND ba.category = 'CUSTOMER' FETCH FIRST 1 ROW ONLY" +
            ") WHERE torihiki_no IS NULL OR torihiki_no LIKE '%（%'");
    }
}
