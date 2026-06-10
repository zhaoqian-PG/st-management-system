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
        jdbcTemplate.update("UPDATE employee SET status = '在職' WHERE status IS NULL");
        jdbcTemplate.update("UPDATE employee SET status = '離職' WHERE leave_date IS NOT NULL AND status = '在職'");
        jdbcTemplate.update("UPDATE employee SET status = '在職' WHERE leave_date IS NULL AND status = '離職'");
        // Reset admin password to plain text for dev login
        jdbcTemplate.update("UPDATE \"user\" SET password = 'admin123' WHERE username = 'admin' AND length(password) > 20");
        // Add purchase_order sample data if empty
        int poCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM purchase_order", Integer.class);
        if (poCount == 0) {
            jdbcTemplate.update("INSERT INTO purchase_order (order_number, customer_id, order_date, delivery_date, subject, recipient_dept, recipient_name, amount, tax_rate, status, remark, create_time, update_time) VALUES ('PO-2026-0001', 1, '2026-05-01', '2026-06-15', 'サーバー機器一式', '情報システム部', '山田 太郎', 5000000.00, 10.00, '発注済', '新規プロジェクト用', NOW(), NOW())");
            jdbcTemplate.update("INSERT INTO purchase_order (order_number, customer_id, order_date, delivery_date, subject, recipient_dept, recipient_name, amount, tax_rate, status, remark, create_time, update_time) VALUES ('PO-2026-0002', 2, '2026-05-10', '2026-06-30', 'ネットワーク工事', '総務部', '佐藤 健一', 2500000.00, 10.00, '下書き', '大阪支店増設', NOW(), NOW())");
            jdbcTemplate.update("INSERT INTO purchase_order (order_number, customer_id, order_date, delivery_date, subject, recipient_dept, recipient_name, amount, tax_rate, tax_amount, total_with_tax, status, remark, create_time, update_time) VALUES ('PO-2026-0003', 3, '2026-05-15', '2026-07-15', 'ソフトウェア開発', '開発部', '田中 美咲', 8000000.00, 10.00, 800000.00, 8800000.00, '検収済', '基幹システム改修', NOW(), NOW())");
            // Update existing purchase_orders with null tax values
            jdbcTemplate.update("UPDATE purchase_order SET tax_amount = ROUND(amount * COALESCE(tax_rate, 10) / 100, 2), total_with_tax = amount + ROUND(amount * COALESCE(tax_rate, 10) / 100, 2) WHERE tax_amount IS NULL");
            // Add detail data for purchase orders
            int podCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM purchase_order_detail", Integer.class);
            if (podCount == 0) {
                jdbcTemplate.update("INSERT INTO purchase_order_detail (order_id, item_name, quantity, unit_price, amount, create_time) SELECT id, 'サーバー本体', 2, 1500000.00, 3000000.00, NOW() FROM purchase_order WHERE order_number = 'PO-2026-0001'");
                jdbcTemplate.update("INSERT INTO purchase_order_detail (order_id, item_name, quantity, unit_price, amount, create_time) SELECT id, 'NASストレージ', 1, 800000.00, 800000.00, NOW() FROM purchase_order WHERE order_number = 'PO-2026-0001'");
                jdbcTemplate.update("INSERT INTO purchase_order_detail (order_id, item_name, quantity, unit_price, amount, create_time) SELECT id, 'ルーター', 3, 500000.00, 1500000.00, NOW() FROM purchase_order WHERE order_number = 'PO-2026-0002'");
                jdbcTemplate.update("INSERT INTO purchase_order_detail (order_id, item_name, quantity, unit_price, amount, create_time) SELECT id, '設計書作成', 1, 3000000.00, 3000000.00, NOW() FROM purchase_order WHERE order_number = 'PO-2026-0003'");
                jdbcTemplate.update("INSERT INTO purchase_order_detail (order_id, item_name, quantity, unit_price, amount, create_time) SELECT id, 'プログラミング', 1, 4000000.00, 4000000.00, NOW() FROM purchase_order WHERE order_number = 'PO-2026-0003'");
            }
        }
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
