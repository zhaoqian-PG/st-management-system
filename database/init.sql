-- ============================================================
-- ST Management System - データベース初期化スクリプト
-- Database: PostgreSQL 16
-- ============================================================

-- ============================================================
-- 既存テーブル削除（依存関係を考慮した順序）
-- ============================================================
DROP TABLE IF EXISTS order_documents CASCADE;
DROP TABLE IF EXISTS invoice CASCADE;
DROP TABLE IF EXISTS attendance CASCADE;
DROP TABLE IF EXISTS bank_account CASCADE;
DROP TABLE IF EXISTS customer CASCADE;
DROP TABLE IF EXISTS employee CASCADE;
DROP TABLE IF EXISTS "user" CASCADE;
DROP SEQUENCE IF EXISTS torihiki_seq CASCADE;
DROP SEQUENCE IF EXISTS customer_code_seq CASCADE;
DROP SEQUENCE IF EXISTS employee_code_seq CASCADE;

-- 取引番号採番シーケンス（MASTERテーブル用）
CREATE SEQUENCE IF NOT EXISTS torihiki_seq START 1;

-- 顧客番号採番シーケンス
CREATE SEQUENCE IF NOT EXISTS customer_code_seq START 1;

-- 社員番号採番シーケンス
CREATE SEQUENCE IF NOT EXISTS employee_code_seq START 1;

-- 1. ログインユーザーテーブル
CREATE TABLE IF NOT EXISTS "user" (
    id          BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'USER',
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "user" IS 'ログインユーザー';
COMMENT ON COLUMN "user".username IS 'ログイン名';
COMMENT ON COLUMN "user".password IS 'パスワード（BCrypt暗号化）';
COMMENT ON COLUMN "user".role IS 'ロール：ADMIN / USER';

-- 2. 社員情報テーブル
CREATE TABLE IF NOT EXISTS employee (
    id                      BIGSERIAL       PRIMARY KEY,
    employee_code           VARCHAR(20)     NOT NULL UNIQUE DEFAULT 'EMP' || LPAD(nextval('employee_code_seq')::text, 4, '0'),
    name                    VARCHAR(100)    NOT NULL,
    email                   VARCHAR(255),
    phone                   VARCHAR(20),
    japan_address           VARCHAR(500),
    china_address           VARCHAR(500),
    china_phone             VARCHAR(20),
    china_emergency_contact VARCHAR(100),
    torihiki_no             VARCHAR(500),
    department              VARCHAR(100),
    position                VARCHAR(100),
    join_date               DATE,
    birth_date              DATE,
    attachment_path         VARCHAR(500),
    create_time             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE employee IS '社員情報';
COMMENT ON COLUMN employee.employee_code IS '社員番号';
COMMENT ON COLUMN employee.japan_address IS '日本住所';
COMMENT ON COLUMN employee.china_address IS '中国住所';
COMMENT ON COLUMN employee.china_phone IS '中国電話番号';
COMMENT ON COLUMN employee.china_emergency_contact IS '中国緊急連絡先';
COMMENT ON COLUMN employee.join_date IS '入社日';
COMMENT ON COLUMN employee.birth_date IS '生年月日';
COMMENT ON COLUMN employee.attachment_path IS '添付ファイルパス';

-- ============================================================
-- 3. customer（顧客）★業務テーブル（先に作成：bank_accountがFK参照するため）
-- ============================================================
CREATE TABLE IF NOT EXISTS customer (
    id                  BIGSERIAL       PRIMARY KEY,
    customer_code       VARCHAR(20)     NOT NULL UNIQUE DEFAULT 'CUS' || LPAD(nextval('customer_code_seq')::text, 4, '0'),
    company_name        VARCHAR(200)    NOT NULL,
    president_name      VARCHAR(100),
    website             VARCHAR(500),
    address             VARCHAR(500),
    contact_name        VARCHAR(100),
    email               VARCHAR(255),
    phone               VARCHAR(20),
    sales_rep_name      VARCHAR(100),
    sales_rep_phone     VARCHAR(20),
    sales_rep_email     VARCHAR(255),
    admin_rep_name      VARCHAR(100),
    admin_rep_phone     VARCHAR(20),
    admin_rep_email     VARCHAR(255),
    torihiki_no         VARCHAR(500),
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE customer IS '顧客情報（業務テーブル）';
COMMENT ON COLUMN customer.torihiki_no IS '紐付く取引番号（bank_account.torihiki_noと連携、複数時カンマ区切り）';
COMMENT ON COLUMN customer.customer_code IS '顧客番号';
COMMENT ON COLUMN customer.company_name IS '会社名';
COMMENT ON COLUMN customer.president_name IS '社長名';
COMMENT ON COLUMN customer.website IS '会社Webサイト';
COMMENT ON COLUMN customer.contact_name IS '窓口担当者';
COMMENT ON COLUMN customer.sales_rep_name IS '営業担当者氏名';
COMMENT ON COLUMN customer.sales_rep_phone IS '営業担当者電話番号';
COMMENT ON COLUMN customer.sales_rep_email IS '営業担当者メールアドレス';
COMMENT ON COLUMN customer.admin_rep_name IS '事務担当者氏名';
COMMENT ON COLUMN customer.admin_rep_phone IS '事務担当者電話番号';
COMMENT ON COLUMN customer.admin_rep_email IS '事務担当者メールアドレス';

-- ============================================================
-- 4. bank_account（銀行口座）★MASTERテーブル
--    取引番号を自動採番（torihiki_seq）。customer_id で顧客に紐付く（1:N）。
--    1つの顧客が複数の銀行口座を持つことができる。
-- ============================================================
CREATE TABLE IF NOT EXISTS bank_account (
    id                  BIGSERIAL       PRIMARY KEY,
    torihiki_no         VARCHAR(20)     NOT NULL UNIQUE DEFAULT 'BK' || LPAD(nextval('torihiki_seq')::text, 6, '0'),
    category            VARCHAR(10)     NOT NULL DEFAULT 'CUSTOMER',
    customer_id         BIGINT,
    employee_id         BIGINT,
    branch_code         VARCHAR(10),
    bank_name           VARCHAR(200)    NOT NULL,
    account_type        VARCHAR(20)     NOT NULL,
    account_number      VARCHAR(50)     NOT NULL,
    account_holder      VARCHAR(100)    NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ba_customer
        FOREIGN KEY (customer_id) REFERENCES customer(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_ba_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_ba_link CHECK ((category = 'CUSTOMER' AND employee_id IS NULL) OR (category = 'EMPLOYEE' AND customer_id IS NULL)),
    CONSTRAINT chk_ba_category CHECK (category IN ('CUSTOMER', 'EMPLOYEE')),
    CONSTRAINT chk_ba_account_type CHECK (account_type IN ('普通', '当座'))
);

COMMENT ON TABLE bank_account IS '銀行口座（MASTERテーブル・機密情報）';
COMMENT ON COLUMN bank_account.torihiki_no IS '取引番号（自動採番、一意）';
COMMENT ON COLUMN bank_account.category IS '区分: CUSTOMER(顧客用) / EMPLOYEE(社員用)';
COMMENT ON COLUMN bank_account.customer_id IS 'FK→customer（category=CUSTOMER時）';
COMMENT ON COLUMN bank_account.employee_id IS 'FK→employee（category=EMPLOYEE時）';
COMMENT ON COLUMN bank_account.branch_code IS '支店番号';
COMMENT ON COLUMN bank_account.bank_name IS '銀行名称';
COMMENT ON COLUMN bank_account.account_type IS '口座種類：普通/当座';
COMMENT ON COLUMN bank_account.account_number IS '口座番号';
COMMENT ON COLUMN bank_account.account_holder IS '口座名義人';

CREATE INDEX idx_ba_customer ON bank_account(customer_id);
CREATE INDEX idx_ba_employee ON bank_account(employee_id);

-- 5. 勤務記録テーブル
CREATE TABLE IF NOT EXISTS attendance (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL,
    work_date       DATE            NOT NULL,
    work_hours      DECIMAL(4,1)    NOT NULL DEFAULT 0,
    overtime_hours  DECIMAL(4,1)    NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT '出勤',
    remark          VARCHAR(500),
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_attendance_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id)
        ON DELETE CASCADE
);

COMMENT ON TABLE attendance IS '勤務記録';
COMMENT ON COLUMN attendance.work_hours IS '勤務時間（h）';
COMMENT ON COLUMN attendance.overtime_hours IS '残業時間（h）';
COMMENT ON COLUMN attendance.status IS 'ステータス：出勤 / 欠勤 / 休暇';

CREATE INDEX idx_attendance_employee_id ON attendance(employee_id);
CREATE INDEX idx_attendance_work_date    ON attendance(work_date);
CREATE INDEX idx_attendance_emp_date     ON attendance(employee_id, work_date);

-- 6. 請求書テーブル
CREATE TABLE IF NOT EXISTS invoice (
    id              BIGSERIAL       PRIMARY KEY,
    invoice_number  VARCHAR(50)     NOT NULL UNIQUE,
    customer_id     BIGINT          NOT NULL,
    year            INT             NOT NULL,
    month           INT             NOT NULL,
    amount          DECIMAL(12,2)   NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT '下書き',
    remark          TEXT,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_invoice_customer
        FOREIGN KEY (customer_id) REFERENCES customer(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_invoice_month CHECK (month BETWEEN 1 AND 12)
);

COMMENT ON TABLE invoice IS '請求書';
COMMENT ON COLUMN invoice.invoice_number IS '請求書番号';
COMMENT ON COLUMN invoice.amount IS '金額（円）';
COMMENT ON COLUMN invoice.status IS 'ステータス：下書き / 送付済 / 入金済';

CREATE INDEX idx_invoice_customer_id ON invoice(customer_id);
CREATE INDEX idx_invoice_year_month   ON invoice(year, month);

-- 7. 社員添付ファイルテーブル
CREATE TABLE IF NOT EXISTS employee_attachment (
    id          BIGSERIAL       PRIMARY KEY,
    employee_id BIGINT          NOT NULL,
    file_name   VARCHAR(255)    NOT NULL,
    file_path   VARCHAR(500)    NOT NULL,
    file_size   BIGINT          NOT NULL DEFAULT 0,
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_emp_att_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id)
        ON DELETE CASCADE
);

COMMENT ON TABLE employee_attachment IS '社員添付ファイル';
CREATE INDEX idx_emp_att_employee ON employee_attachment(employee_id);

-- 8. 注文書テーブル（添付資料）
CREATE TABLE IF NOT EXISTS order_documents (
    id          BIGSERIAL       PRIMARY KEY,
    invoice_id  BIGINT          NOT NULL,
    file_name   VARCHAR(255)    NOT NULL,
    file_path   VARCHAR(500)    NOT NULL,
    file_size   BIGINT          NOT NULL DEFAULT 0,
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_documents_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoice(id)
        ON DELETE CASCADE
);

COMMENT ON TABLE order_documents IS '注文書（添付資料）';
COMMENT ON COLUMN order_documents.file_name IS 'ファイル名';
COMMENT ON COLUMN order_documents.file_path IS '保存パス';
COMMENT ON COLUMN order_documents.file_size IS 'ファイルサイズ（byte）';

CREATE INDEX idx_order_documents_invoice_id ON order_documents(invoice_id);

-- ============================================================
-- 初期データ
-- ============================================================

-- デフォルト管理者（パスワード: admin123、BCrypt暗号化）
INSERT INTO "user" (username, password, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN')
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- サンプルデータ
-- ============================================================

-- 社員（5件）
INSERT INTO employee (employee_code, name, email, phone, japan_address, china_address,
    china_phone, china_emergency_contact, department, position, join_date, birth_date, torihiki_no)
VALUES
    ('EMP0001', '山田 太郎', 'yamada@example.com', '090-1111-2222',
     '東京都新宿区西新宿1-1-1', '上海市浦东新区世纪大道100号',
     '138-0001-0001', '山田 花子（妻）', '営業部', '課長', '2020-04-01', '1985-06-15',
     '三菱UFJ銀行（038）'),
    ('EMP0002', '鈴木 花子', 'suzuki@example.com', '090-3333-4444',
     '東京都港区赤坂2-2-2', '北京市朝阳区建国路88号',
     '138-0002-0002', '鈴木 一郎（夫）', '技術部', '一般社員', '2021-04-01', '1990-03-22',
     'みずほ銀行（890）'),
    ('EMP0003', '佐藤 健一', 'sato@example.com', '090-5555-6666',
     '東京都千代田区丸の内3-3-3', '广州市天河区体育西路100号',
     '138-0003-0003', '佐藤 良子（母）', '経理部', '係長', '2019-04-01', '1982-11-08',
     '三井住友銀行（056）'),
    ('EMP0004', '田中 美咲', 'tanaka@example.com', '090-7777-8888',
     '東京都渋谷区道玄坂4-4-4', '深圳市南山区科技园路50号',
     '138-0004-0004', '田中 健太（父）', '人事部', '部長', '2018-04-01', '1980-01-30',
     'りそな銀行（001）'),
    ('EMP0005', '伊藤 大輔', 'ito@example.com', '090-9999-0000',
     '東京都品川区大崎5-5-5', '大连市沙河口区西安路20号',
     '138-0005-0005', '伊藤 由美（母）', '営業部', '一般社員', '2022-04-01', '1995-09-12',
     '三菱UFJ銀行（038）')
ON CONFLICT (employee_code) DO NOTHING;

-- 社員番号シーケンスをサンプルデータの次に設定
SELECT setval('employee_code_seq', 5);

-- 社員用銀行口座
INSERT INTO bank_account (torihiki_no, category, employee_id, branch_code, bank_name, account_type, account_number, account_holder)
VALUES
    ('BK000101', 'EMPLOYEE', 1, '038', '三菱UFJ銀行', '普通', '1234567', 'ヤマダ タロウ'),
    ('BK000102', 'EMPLOYEE', 2, '890', 'みずほ銀行', '普通', '2345678', 'スズキ ハナコ'),
    ('BK000103', 'EMPLOYEE', 3, '056', '三井住友銀行', '当座', '3456789', 'サトウ ケンイチ'),
    ('BK000104', 'EMPLOYEE', 4, '001', 'りそな銀行', '普通', '4567890', 'タナカ ミサキ'),
    ('BK000105', 'EMPLOYEE', 5, '038', '三菱UFJ銀行', '普通', '5678901', 'イトウ ダイスケ');

SELECT setval('torihiki_seq', 200);

-- 顧客（4件）★業務テーブル
INSERT INTO customer (customer_code, company_name, president_name, website, address,
    contact_name, email, phone,
    sales_rep_name, sales_rep_phone, sales_rep_email,
    admin_rep_name, admin_rep_phone, admin_rep_email, torihiki_no)
VALUES
    ('CUS0001', '株式会社テクノ東京', '田中 宏', 'www.techno-tokyo.co.jp',
     '東京都千代田区丸の内1-1-1',
     '中村 博', 'info@techno-tokyo.co.jp', '03-1111-2222',
     '中村 博', '090-1234-5678', 'nakamura@techno-tokyo.co.jp',
     '斉藤 雅子', '03-1111-2223', 'saito@techno-tokyo.co.jp',
     '三菱UFJ銀行（038）、みずほ銀行（890）'),
    ('CUS0002', '大阪商事株式会社', '山本 和夫', 'www.osaka-shoji.jp',
     '大阪府大阪市中央区本町1-1-1',
     '加藤 恵', 'info@osaka-shoji.jp', '06-3333-4444',
     '加藤 恵', '090-2345-6789', 'kato@osaka-shoji.jp',
     '井上 隆', '06-3333-4445', 'inoue@osaka-shoji.jp',
     '三菱UFJ銀行（056）'),
    ('CUS0003', '名古屋工業有限会社', '伊藤 正和', 'www.nagoya-kogyo.jp',
     '愛知県名古屋市中区栄2-2-2',
     '渡辺 誠', 'info@nagoya-kogyo.jp', '052-5555-6666',
     '渡辺 誠', '090-3456-7890', 'watanabe@nagoya-kogyo.jp',
     '木村 由美', '052-5555-6667', 'kimura@nagoya-kogyo.jp',
     '三井住友銀行（123）'),
    ('CUS0004', '福岡システム株式会社', '吉田 和彦', 'www.fukuoka-sys.jp',
     '福岡県福岡市博多区博多駅前3-3-3',
     '松本 直子', 'info@fukuoka-sys.jp', '092-7777-8888',
     '松本 直子', '090-4567-8901', 'matsumoto@fukuoka-sys.jp',
     '小川 健太', '092-7777-8889', 'ogawa@fukuoka-sys.jp',
     NULL)
ON CONFLICT (customer_code) DO NOTHING;

-- 顧客番号シーケンスをサンプルデータの次に設定
SELECT setval('customer_code_seq', 4);

-- 銀行口座（5件）★MASTERテーブル / 1顧客:多口座
-- CUS001（テクノ東京）→ 2口座、CUS002（大阪商事）→ 1口座、CUS003（名古屋工業）→ 1口座
-- CUS004（福岡システム）→ 未紐付（customer_id = NULL）
INSERT INTO bank_account (torihiki_no, category, customer_id, branch_code, bank_name, account_type, account_number, account_holder)
VALUES
    ('BK000001', 'CUSTOMER', 1, '038', '三菱UFJ銀行', '普通', '7654321', 'カ）テクノトウキョウ'),
    ('BK000002', 'CUSTOMER', 1, '890', 'みずほ銀行', '当座', '1122334', 'カ）テクノトウキョウ'),
    ('BK000003', 'CUSTOMER', 2, '056', '三菱UFJ銀行', '当座', '8765432', 'カ）オオサカショウジ'),
    ('BK000004', 'CUSTOMER', 3, '123', '三井住友銀行', '普通', '9876543', 'ユ）ナゴヤコウギョウ'),
    ('BK000005', 'CUSTOMER', NULL, '456', '福岡銀行', '普通', '0987654', 'カ）フクオカシステム');

-- 勤務記録（5件）
INSERT INTO attendance (employee_id, work_date, work_hours, overtime_hours, status, remark)
VALUES
    (1, '2026-05-01', 8.0, 0.0, '出勤', NULL),
    (2, '2026-05-01', 8.0, 2.5, '出勤', 'システムメンテナンス対応'),
    (3, '2026-05-01', 0.0, 0.0, '欠勤', '体調不良のため'),
    (1, '2026-05-02', 8.0, 1.0, '出勤', NULL),
    (2, '2026-05-02', 0.0, 0.0, '休暇', '年次有給休暇');

-- 請求書（3件）
INSERT INTO invoice (invoice_number, customer_id, year, month, amount, status, remark)
VALUES
    ('INV-2026-0501', 1, 2026, 5, 1500000.00, '下書き', 'システム開発費用'),
    ('INV-2026-0502', 2, 2026, 5,  850000.00, '送付済', 'サーバー保守費用'),
    ('INV-2026-0401', 3, 2026, 4, 2300000.00, '入金済', 'ソフトウェアライセンス');

-- 注文書（2件 / INV-2026-0501 に紐付く）
INSERT INTO order_documents (invoice_id, file_name, file_path, file_size)
VALUES
    (1, '注文書_20260501.pdf', 'invoices/2026/05/order_20260501.pdf', 245760),
    (1, '仕様書_v2.xlsx',       'invoices/2026/05/spec_v2.xlsx',       184320);
