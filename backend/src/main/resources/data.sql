UPDATE bank_account SET category = 'CUSTOMER' WHERE category IS NULL AND customer_id IS NOT NULL;
UPDATE bank_account SET category = 'EMPLOYEE' WHERE category IS NULL AND employee_id IS NOT NULL;
