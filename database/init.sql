-- ============================================================
-- ST Management System - データベース初期化スクリプト
-- Database: PostgreSQL 16
-- ============================================================

-- ============================================================
-- 既存テーブル削除（依存関係を考慮した順序）
-- ============================================================
DROP TABLE IF EXISTS purchase_order_detail CASCADE;
DROP TABLE IF EXISTS purchase_order CASCADE;
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
    employee_id BIGINT,
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'USER'))
);

COMMENT ON TABLE "user" IS 'ログインユーザー（権限管理）';
COMMENT ON COLUMN "user".username IS 'ログイン名';
COMMENT ON COLUMN "user".password IS 'パスワード（BCrypt暗号化）';
COMMENT ON COLUMN "user".role IS 'ロール：ADMIN（管理者）/ USER（一般）';
COMMENT ON COLUMN "user".employee_id IS 'FK→employee(id) 1:1社員紐付け';

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
    status                  VARCHAR(10)     NOT NULL DEFAULT '在職',
    department              VARCHAR(100),
    position                VARCHAR(100),
    join_date               DATE,
    birth_date              DATE,
    leave_date              DATE,
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
COMMENT ON COLUMN employee.leave_date IS '離職日';

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
    torihiki_no         VARCHAR(20)     NOT NULL DEFAULT 'BK' || LPAD(nextval('torihiki_seq')::text, 6, '0'),
    branch_no           VARCHAR(3)      NOT NULL,
    category            VARCHAR(10)     NOT NULL DEFAULT 'CUSTOMER',
    customer_id         BIGINT,
    employee_id         BIGINT,
    branch_code         VARCHAR(10),
    bank_name           VARCHAR(200)    NOT NULL,
    account_type        VARCHAR(20)     NOT NULL,
    account_number      VARCHAR(50)     NOT NULL,
    account_holder      VARCHAR(100)    NOT NULL,
    is_default          BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ba_customer
        FOREIGN KEY (customer_id) REFERENCES customer(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_ba_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id)
        ON DELETE SET NULL,
    CONSTRAINT uk_ba_torihiki_branch UNIQUE (torihiki_no, branch_no),
    CONSTRAINT chk_ba_link CHECK ((category = 'CUSTOMER' AND employee_id IS NULL) OR (category = 'EMPLOYEE' AND customer_id IS NULL)),
    CONSTRAINT chk_ba_category CHECK (category IN ('CUSTOMER', 'EMPLOYEE')),
    CONSTRAINT chk_ba_account_type CHECK (account_type IN ('普通', '当座'))
);

COMMENT ON TABLE bank_account IS '銀行口座（MASTERテーブル・機密情報）';
COMMENT ON COLUMN bank_account.torihiki_no IS '取引番号（グループキー）';
COMMENT ON COLUMN bank_account.branch_no IS '枝番（取引番号+枝番で一意）';
COMMENT ON COLUMN bank_account.category IS '区分: CUSTOMER(顧客用) / EMPLOYEE(社員用)';
COMMENT ON COLUMN bank_account.customer_id IS 'FK→customer（category=CUSTOMER時）';
COMMENT ON COLUMN bank_account.employee_id IS 'FK→employee（category=EMPLOYEE時）';
COMMENT ON COLUMN bank_account.branch_code IS '支店番号';
COMMENT ON COLUMN bank_account.bank_name IS '銀行名称';
COMMENT ON COLUMN bank_account.account_type IS '口座種類：普通/当座';
COMMENT ON COLUMN bank_account.account_number IS '口座番号';
COMMENT ON COLUMN bank_account.account_holder IS '口座名義人';

CREATE INDEX IF NOT EXISTS idx_ba_customer ON bank_account(customer_id);
CREATE INDEX IF NOT EXISTS idx_ba_employee ON bank_account(employee_id);
CREATE INDEX IF NOT EXISTS idx_ba_torihiki ON bank_account(torihiki_no);

-- 5. 勤務記録テーブル
CREATE TABLE IF NOT EXISTS attendance (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL,
    work_date       DATE            NOT NULL,
    work_hours      DECIMAL(4,1)    NOT NULL DEFAULT 0,
    overtime_hours  DECIMAL(4,1)    NOT NULL DEFAULT 0,
    clock_in        TIME,
    clock_out       TIME,
    total_hours     DECIMAL(4,1),
    work_type       VARCHAR(20)     NOT NULL DEFAULT 'NORMAL',
    status          VARCHAR(20)     NOT NULL DEFAULT '出勤',
    remark          VARCHAR(500),
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_attendance_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_attendance_work_type CHECK (work_type IN ('NORMAL', 'REMOTE', 'HOLIDAY_WORK', 'LEAVE'))
);

COMMENT ON TABLE attendance IS '勤務記録';
COMMENT ON COLUMN attendance.work_hours IS '勤務時間（h）';
COMMENT ON COLUMN attendance.overtime_hours IS '残業時間（h）';
COMMENT ON COLUMN attendance.clock_in IS '出勤打刻時刻';
COMMENT ON COLUMN attendance.clock_out IS '退勤打刻時刻';
COMMENT ON COLUMN attendance.total_hours IS '総労働時間（h）';
COMMENT ON COLUMN attendance.work_type IS '勤務区分: NORMAL(通常)/REMOTE(在宅)/HOLIDAY_WORK(休日出勤)/LEAVE(休暇)';
COMMENT ON COLUMN attendance.status IS 'ステータス：出勤 / 欠勤 / 休暇';
COMMENT ON COLUMN attendance.total_hours IS '総労働時間（h）';

CREATE INDEX IF NOT EXISTS idx_attendance_employee_id ON attendance(employee_id);
CREATE INDEX IF NOT EXISTS idx_attendance_work_date    ON attendance(work_date);
CREATE INDEX IF NOT EXISTS idx_attendance_emp_date     ON attendance(employee_id, work_date);

-- 6. 請求書テーブル
CREATE TABLE IF NOT EXISTS invoice (
    id              BIGSERIAL       PRIMARY KEY,
    invoice_number  VARCHAR(50)     NOT NULL UNIQUE,
    customer_id     BIGINT          NOT NULL,
    year            INT             NOT NULL,
    month           INT             NOT NULL,
    invoice_date    DATE,
    due_date        DATE,
    amount          DECIMAL(12,2)   NOT NULL DEFAULT 0,
    tax_rate        DECIMAL(4,2)    DEFAULT 10.00,
    tax_amount      DECIMAL(12,2)   DEFAULT 0,
    total_with_tax  DECIMAL(12,2)   DEFAULT 0,
    subject         VARCHAR(500),
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
COMMENT ON COLUMN invoice.invoice_date IS '請求日';
COMMENT ON COLUMN invoice.due_date IS '支払期限';
COMMENT ON COLUMN invoice.amount IS '請求金額（税抜）';
COMMENT ON COLUMN invoice.tax_rate IS '消費税率（%）';
COMMENT ON COLUMN invoice.tax_amount IS '消費税額';
COMMENT ON COLUMN invoice.total_with_tax IS '税込合計金額';
COMMENT ON COLUMN invoice.subject IS '件名・プロジェクト名';
COMMENT ON COLUMN invoice.status IS 'ステータス：下書き / 送付済 / 入金済';

CREATE INDEX IF NOT EXISTS idx_invoice_customer_id ON invoice(customer_id);
CREATE INDEX IF NOT EXISTS idx_invoice_year_month   ON invoice(year, month);

-- 7. 請求明細テーブル
CREATE TABLE IF NOT EXISTS invoice_detail (
    id          BIGSERIAL       PRIMARY KEY,
    invoice_id  BIGINT          NOT NULL,
    employee_id BIGINT,
    employee_name VARCHAR(100),
    description VARCHAR(500),
    quantity    DECIMAL(8,2)    NOT NULL DEFAULT 1,
    unit_price  DECIMAL(12,2)   NOT NULL DEFAULT 0,
    amount      DECIMAL(12,2)   NOT NULL DEFAULT 0,
    is_overtime BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inv_det_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_det_employee FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE SET NULL
);

COMMENT ON TABLE invoice_detail IS '請求明細（複数人対応）';
COMMENT ON COLUMN invoice_detail.description IS '項目名';
COMMENT ON COLUMN invoice_detail.unit_price IS '単価（時間単価など）';
COMMENT ON COLUMN invoice_detail.is_overtime IS '残業フラグ';

CREATE INDEX IF NOT EXISTS idx_inv_det_invoice ON invoice_detail(invoice_id);

-- 8. 社員添付ファイルテーブル
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
CREATE INDEX IF NOT EXISTS idx_emp_att_employee ON employee_attachment(employee_id);

-- 9. 注文書テーブル
CREATE TABLE IF NOT EXISTS purchase_order (
    id              BIGSERIAL       PRIMARY KEY,
    order_number    VARCHAR(50)     NOT NULL UNIQUE,
    customer_id     BIGINT          NOT NULL,
    order_date      DATE            NOT NULL,
    delivery_date   DATE,
    recipient_dept  VARCHAR(100),
    recipient_name  VARCHAR(100),
    recipient_addr  VARCHAR(500),
    recipient_tel   VARCHAR(20),
    subject         VARCHAR(500),
    amount          DECIMAL(12,2)   NOT NULL DEFAULT 0,
    tax_rate        DECIMAL(4,2)    DEFAULT 10.00,
    tax_amount      DECIMAL(12,2)   DEFAULT 0,
    total_with_tax  DECIMAL(12,2)   DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT '下書き',
    remark          TEXT,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_po_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);

COMMENT ON TABLE purchase_order IS '注文書';
COMMENT ON COLUMN purchase_order.order_number IS '注文番号';
COMMENT ON COLUMN purchase_order.order_date IS '注文日';
COMMENT ON COLUMN purchase_order.delivery_date IS '納品期限';
COMMENT ON COLUMN purchase_order.recipient_dept IS '発注先部署';
COMMENT ON COLUMN purchase_order.recipient_name IS '発注先担当者';
COMMENT ON COLUMN purchase_order.recipient_addr IS '発注先住所';
COMMENT ON COLUMN purchase_order.recipient_tel IS '発注先TEL';
COMMENT ON COLUMN purchase_order.status IS '下書き/発注済/納品済/検収済';

CREATE INDEX IF NOT EXISTS idx_po_customer ON purchase_order(customer_id);

CREATE TABLE IF NOT EXISTS purchase_order_detail (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL,
    employee_name   VARCHAR(100),
    item_name       VARCHAR(500)    NOT NULL,
    quantity        DECIMAL(8,2)    NOT NULL DEFAULT 1,
    unit_price      DECIMAL(12,2)   NOT NULL DEFAULT 0,
    amount          DECIMAL(12,2)   NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pod_order FOREIGN KEY (order_id) REFERENCES purchase_order(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pod_order ON purchase_order_detail(order_id);

-- 10. 注文書添付テーブル（旧注文書テーブル）
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

CREATE INDEX IF NOT EXISTS idx_order_documents_invoice_id ON order_documents(invoice_id);

-- user.employee_id 外部キー（employeeテーブル作成後に追加）
ALTER TABLE "user" ADD CONSTRAINT fk_user_employee
    FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE SET NULL;

-- ============================================================
-- 初期データ
-- ============================================================

-- デフォルト管理者（パスワード: admin123、BCrypt暗号化）
INSERT INTO "user" (username, password, role)
VALUES ('admin', 'admin123', 'ADMIN')
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
     'BK000101'),
    ('EMP0002', '鈴木 花子', 'suzuki@example.com', '090-3333-4444',
     '東京都港区赤坂2-2-2', '北京市朝阳区建国路88号',
     '138-0002-0002', '鈴木 一郎（夫）', '技術部', '一般社員', '2021-04-01', '1990-03-22',
     'BK000102'),
    ('EMP0003', '佐藤 健一', 'sato@example.com', '090-5555-6666',
     '東京都千代田区丸の内3-3-3', '广州市天河区体育西路100号',
     '138-0003-0003', '佐藤 良子（母）', '経理部', '係長', '2019-04-01', '1982-11-08',
     'BK000103'),
    ('EMP0004', '田中 美咲', 'tanaka@example.com', '090-7777-8888',
     '東京都渋谷区道玄坂4-4-4', '深圳市南山区科技园路50号',
     '138-0004-0004', '田中 健太（父）', '人事部', '部長', '2018-04-01', '1980-01-30',
     'BK000104'),
    ('EMP0005', '伊藤 大輔', 'ito@example.com', '090-9999-0000',
     '東京都品川区大崎5-5-5', '大连市沙河口区西安路20号',
     '138-0005-0005', '伊藤 由美（母）', '営業部', '一般社員', '2022-04-01', '1995-09-12',
     'BK000105')
ON CONFLICT (employee_code) DO NOTHING;

-- 社員番号シーケンスをサンプルデータの次に設定
SELECT setval('employee_code_seq', 5);

-- 社員用銀行口座
INSERT INTO bank_account (torihiki_no, branch_no, category, employee_id, branch_code, bank_name, account_type, account_number, account_holder, is_default)
VALUES
    ('BK000101', '001', 'EMPLOYEE', 1, '038', '三菱UFJ銀行', '普通', '1234567', 'ヤマダ タロウ', TRUE),
    ('BK000102', '001', 'EMPLOYEE', 2, '890', 'みずほ銀行', '普通', '2345678', 'スズキ ハナコ', TRUE),
    ('BK000103', '001', 'EMPLOYEE', 3, '056', '三井住友銀行', '当座', '3456789', 'サトウ ケンイチ', TRUE),
    ('BK000104', '001', 'EMPLOYEE', 4, '001', 'りそな銀行', '普通', '4567890', 'タナカ ミサキ', TRUE),
    ('BK000105', '001', 'EMPLOYEE', 5, '038', '三菱UFJ銀行', '普通', '5678901', 'イトウ ダイスケ', TRUE);

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
     'BK000001'),
    ('CUS0002', '大阪商事株式会社', '山本 和夫', 'www.osaka-shoji.jp',
     '大阪府大阪市中央区本町1-1-1',
     '加藤 恵', 'info@osaka-shoji.jp', '06-3333-4444',
     '加藤 恵', '090-2345-6789', 'kato@osaka-shoji.jp',
     '井上 隆', '06-3333-4445', 'inoue@osaka-shoji.jp',
     'BK000002'),
    ('CUS0003', '名古屋工業有限会社', '伊藤 正和', 'www.nagoya-kogyo.jp',
     '愛知県名古屋市中区栄2-2-2',
     '渡辺 誠', 'info@nagoya-kogyo.jp', '052-5555-6666',
     '渡辺 誠', '090-3456-7890', 'watanabe@nagoya-kogyo.jp',
     '木村 由美', '052-5555-6667', 'kimura@nagoya-kogyo.jp',
     'BK000003'),
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
INSERT INTO bank_account (torihiki_no, branch_no, category, customer_id, branch_code, bank_name, account_type, account_number, account_holder, is_default)
VALUES
    ('BK000001', '001', 'CUSTOMER', 1, '038', '三菱UFJ銀行', '普通', '7654321', 'カ）テクノトウキョウ', TRUE),
    ('BK000001', '002', 'CUSTOMER', 1, '890', 'みずほ銀行', '当座', '1122334', 'カ）テクノトウキョウ', FALSE),
    ('BK000002', '001', 'CUSTOMER', 2, '056', '三菱UFJ銀行', '当座', '8765432', 'カ）オオサカショウジ', TRUE),
    ('BK000003', '001', 'CUSTOMER', 3, '123', '三井住友銀行', '普通', '9876543', 'ユ）ナゴヤコウギョウ', TRUE),
    ('BK000004', '001', 'CUSTOMER', NULL, '456', '福岡銀行', '普通', '0987654', 'カ）フクオカシステム', FALSE);

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

-- 注文書（3件）
INSERT INTO purchase_order (order_number, customer_id, order_date, delivery_date, subject, recipient_dept, recipient_name, amount, tax_rate, status, remark, create_time, update_time)
VALUES
    ('PO-2026-0001', 1, '2026-05-01', '2026-06-15', 'サーバー機器一式', '情報システム部', '山田 太郎', 5000000.00, 10.00, '発注済', '新規プロジェクト用', NOW(), NOW()),
    ('PO-2026-0002', 2, '2026-05-10', '2026-06-30', 'ネットワーク工事', '総務部', '佐藤 健一', 2500000.00, 10.00, '下書き', '大阪支店増設', NOW(), NOW()),
    ('PO-2026-0003', 3, '2026-05-15', '2026-07-15', 'ソフトウェア開発', '開発部', '田中 美咲', 8000000.00, 10.00, '検収済', '基幹システム改修', NOW(), NOW());

INSERT INTO purchase_order_detail (order_id, employee_name, item_name, quantity, unit_price, amount) VALUES
    (1, '山田 太郎', 'サーバー本体', 2, 1500000.00, 3000000.00),
    (1, '山田 太郎', 'NASストレージ', 1, 800000.00, 800000.00),
    (1, '鈴木 花子', 'UPS電源', 2, 600000.00, 1200000.00),
    (2, '佐藤 健一', 'ルーター', 3, 500000.00, 1500000.00),
    (2, '佐藤 健一', 'LANケーブル', 100, 10000.00, 1000000.00),
    (3, '田中 美咲', '設計書作成', 1, 3000000.00, 3000000.00),
    (3, '田中 美咲', 'プログラミング', 1, 4000000.00, 4000000.00),
    (3, '伊藤 大輔', 'テスト', 1, 1000000.00, 1000000.00);

-- 注文書添付（2件 / INV-2026-0501 に紐付く）
INSERT INTO order_documents (invoice_id, file_name, file_path, file_size)
VALUES
    (1, '注文書_20260501.pdf', 'invoices/2026/05/order_20260501.pdf', 245760),
    (1, '仕様書_v2.xlsx',       'invoices/2026/05/spec_v2.xlsx',       184320);
