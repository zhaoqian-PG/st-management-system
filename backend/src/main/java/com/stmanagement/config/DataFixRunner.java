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
        jdbcTemplate.update("UPDATE bank_account SET category = 'CUSTOMER' WHERE category IS NULL AND customer_id IS NOT NULL");
        jdbcTemplate.update("UPDATE bank_account SET category = 'EMPLOYEE' WHERE category IS NULL AND employee_id IS NOT NULL");
    }
}
