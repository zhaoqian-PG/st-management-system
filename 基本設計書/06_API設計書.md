# ST管理システム API設計書

**版数**: 1.0  
**作成日**: 2026/05/27  
**ベースURL**: `http://localhost:8080/api`  
**認証方式**: Bearer JWT Token  

---

## 1. 共通仕様

### 1.1 リクエストヘッダー

| ヘッダー名 | 値 | 必須 | 説明 |
|-----------|------|:--:|------|
| Content-Type | application/json | ○ | リクエストボディのメディア種別 |
| Authorization | Bearer {jwt_token} | ※ | JWT認証トークン（ログインAPI以外） |

### 1.2 レスポンス形式

**成功時:**
```json
{
  "data": { ... },
  "message": "success"
}
```

**エラー時:**
```json
{
  "error": "エラーメッセージ",
  "status": 400
}
```

### 1.3 HTTPステータスコード

| コード | 説明 |
|:------:|------|
| 200 | 成功 (GET, PUT, DELETE) |
| 201 | 作成成功 (POST) |
| 400 | リクエスト不正（バリデーションエラー） |
| 401 | 認証エラー（トークン切れ/不正） |
| 403 | 権限エラー |
| 404 | リソース未検出 |
| 500 | サーバー内部エラー |

---

## 2. API一覧

### 2.1 認証 API

#### POST /api/auth/login — ログイン

| 項目 | 内容 |
|------|------|
| 認証 | 不要 |
| 説明 | ユーザー名・パスワードで認証し、JWTトークンを返却 |

**リクエストボディ:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**レスポンス (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "admin",
  "role": "ADMIN"
}
```

#### GET /api/auth/me — 現在ユーザー取得

| 項目 | 内容 |
|------|------|
| 認証 | 必要 |
| 説明 | JWTトークンから現在のログインユーザー情報を返却 |

---

### 2.2 社員管理 API

#### GET /api/employee — 社員一覧取得

| 項目 | 内容 |
|------|------|
| クエリパラメータ | `keyword` (社員番号/氏名 部分一致), `department` (部署), `page`, `size` |
| レスポンス | 社員リスト（ページング付き） |

**レスポンス例 (200):**
```json
{
  "data": {
    "content": [{
      "id": 1,
      "employeeCode": "EMP001",
      "name": "山田 太郎",
      "email": "yamada@example.com",
      "phone": "090-1111-2222",
      "address": "東京都新宿区...",
      "department": "営業部",
      "position": "課長",
      "bankName": "三菱UFJ銀行",
      "bankBranch": "新宿支店",
      "bankAccountType": "普通",
      "bankAccountNumber": "1234567",
      "bankAccountHolder": "ヤマダ タロウ",
      "attachmentPath": "/uploads/emp001_resume.pdf",
      "createTime": "2026-05-01T10:00:00"
    }],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0,
    "size": 10
  }
}
```

#### POST /api/employee — 社員新規登録

| 項目 | 内容 |
|------|------|
| リクエストボディ | Employeeオブジェクト（JSON） |
| レスポンス | 登録された社員情報 (201) |

#### GET /api/employee/{id} — 社員詳細取得

#### PUT /api/employee/{id} — 社員情報更新

#### DELETE /api/employee/{id} — 社員削除

#### POST /api/employee/{id}/upload — 添付ファイルアップロード

| 項目 | 内容 |
|------|------|
| Content-Type | multipart/form-data |
| パラメータ | `file` (アップロードファイル) |

#### GET /api/employee/{id}/download — 添付ファイルダウンロード

---

### 2.3 顧客管理 API

#### GET /api/customer — 顧客一覧取得

| 項目 | 内容 |
|------|------|
| クエリパラメータ | `keyword` (顧客番号/会社名), `page`, `size` |
| レスポンス | 顧客リスト（ページング付き） |

#### POST /api/customer — 顧客新規登録

#### GET /api/customer/{id} — 顧客詳細取得

#### PUT /api/customer/{id} — 顧客情報更新

#### DELETE /api/customer/{id} — 顧客削除

（紐付く銀行口座情報も合わせて削除）

#### GET /api/customer/{id}/bank-accounts — 顧客銀行口座一覧取得

| 項目 | 内容 |
|------|------|
| 説明 | 指定顧客に紐付くすべての銀行口座情報を返却 |
| 認証 | 必要（機密情報のため管理者のみ推奨） |

**レスポンス例 (200):**
```json
{
  "data": [{
    "id": 1,
    "customerId": 1,
    "bankName": "みずほ銀行",
    "bankBranch": "東京中央支店",
    "bankAccountType": "普通",
    "bankAccountNumber": "7654321",
    "bankAccountHolder": "カ）テクノトウキョウ"
  }, {
    "id": 2,
    "customerId": 1,
    "bankName": "三菱UFJ銀行",
    "bankBranch": "丸の内支店",
    "bankAccountType": "当座",
    "bankAccountNumber": "1122334",
    "bankAccountHolder": "カ）テクノトウキョウ"
  }]
}
```

#### POST /api/customer/{id}/bank-accounts — 銀行口座追加

| 項目 | 内容 |
|------|------|
| 説明 | 指定顧客に新しい銀行口座を追加 |
| 認証 | 必要 |

**リクエストボディ:**
```json
{
  "bankName": "三井住友銀行",
  "bankBranch": "銀座支店",
  "bankAccountType": "普通",
  "bankAccountNumber": "9988776",
  "bankAccountHolder": "カ）テクノトウキョウ"
}
```

#### PUT /api/customer/{id}/bank-accounts/{accountId} — 銀行口座更新

#### DELETE /api/customer/{id}/bank-accounts/{accountId} — 銀行口座削除

---

### 2.4 勤務管理 API

#### GET /api/attendance — 勤務一覧取得

| 項目 | 内容 |
|------|------|
| クエリパラメータ | `year`, `month`, `employeeId`, `page`, `size` |
| レスポンス | 勤務記録リスト（ページング付き） |

**レスポンス例 (200):**
```json
{
  "data": {
    "content": [{
      "id": 1,
      "employeeId": 1,
      "employeeName": "山田 太郎",
      "workDate": "2026-05-01",
      "workHours": 8.0,
      "overtimeHours": 0.0,
      "status": "出勤",
      "remark": null
    }],
    "totalElements": 5,
    "totalPages": 1
  }
}
```

#### POST /api/attendance — 勤務新規登録

#### PUT /api/attendance/{id} — 勤務情報更新

#### DELETE /api/attendance/{id} — 勤務削除

#### GET /api/attendance/export — CSVエクスポート

| 項目 | 内容 |
|------|------|
| クエリパラメータ | `year`, `month`, `employeeId` |
| レスポンス | CSVファイルダウンロード |
| Content-Type | text/csv |

---

### 2.5 請求書・注文書管理 API

#### GET /api/invoice — 請求書一覧取得

| 項目 | 内容 |
|------|------|
| クエリパラメータ | `year`, `month`, `customerId`, `page`, `size` |
| レスポンス | 請求書リスト（ページング付き） |

**レスポンス例 (200):**
```json
{
  "data": {
    "content": [{
      "id": 1,
      "invoiceNumber": "INV-2026-0501",
      "customerId": 1,
      "customerName": "株式会社テクノ東京",
      "year": 2026,
      "month": 5,
      "amount": 1500000,
      "status": "下書き",
      "remark": "システム開発費用",
      "createTime": "2026-05-01T09:00:00"
    }],
    "totalElements": 3,
    "totalPages": 1
  }
}
```

#### POST /api/invoice — 請求書新規登録

#### GET /api/invoice/{id} — 請求書詳細取得

（注文書一覧も含む）

#### PUT /api/invoice/{id} — 請求書情報更新

#### DELETE /api/invoice/{id} — 請求書削除

（紐付く注文書も削除）

#### POST /api/invoice/{id}/documents — 注文書アップロード

| 項目 | 内容 |
|------|------|
| Content-Type | multipart/form-data |
| パラメータ | `file` (アップロードファイル) |

#### GET /api/invoice/{id}/documents/{docId}/download — 注文書ダウンロード

#### DELETE /api/invoice/{id}/documents/{docId} — 注文書削除

---

## 3. バリデーションルール

| エンティティ | 項目 | ルール |
|-------------|------|--------|
| employee | employee_code | 必須、一意、最大20文字 |
| employee | name | 必須、最大100文字 |
| employee | email | メール形式、最大255文字 |
| customer | customer_code | 必須、一意、最大20文字 |
| customer | company_name | 必須、最大200文字 |
| bank_account | torihiki_no | 自動採番、一意、フォーマット BKxxxxxx（BK+6桁数字） |
| bank_account | branch_code | 最大10文字 |
| bank_account | bank_name | 必須、最大200文字 |
| bank_account | account_type | 必須、「普通」「当座」のいずれか |
| bank_account | account_number | 必須、最大50文字 |
| bank_account | account_holder | 必須、最大100文字 |
| customer | torihiki_no | FK → bank_account(torihiki_no)、SET NULL ON DELETE |
| attendance | work_date | 必須、日付形式 |
| attendance | work_hours | 0～24、小数点1桁 |
| attendance | status | 必須、「出勤」「欠勤」「休暇」のいずれか |
| invoice | invoice_number | 必須、一意、最大50文字 |
| invoice | customer_id | 必須、存在する顧客ID |
| invoice | amount | 必須、0以上 |
| invoice | status | 必須、「下書き」「送付済」「入金済」のいずれか |
