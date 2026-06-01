# ST管理システム API詳細設計書

**版数**: 1.0
**作成日**: 2026/05/27
**ベース**: 基本設計書 API設計書

---

## 1. APIエンドポイント一覧（完全版）

### 1.1 全エンドポイント

| # | Method | Path | 認証 | 機能ID | 説明 |
|:--|--------|------|:--:|--------|------|
| 1 | POST | /api/auth/login | - | F-AUTH-01 | ログイン |
| 2 | GET | /api/auth/me | ○ | F-AUTH-03 | 現在ユーザー取得 |
| 3 | GET | /api/employee | ○ | F-EMP-01/02 | 社員一覧取得・検索 |
| 4 | POST | /api/employee | ○ | F-EMP-03 | 社員新規登録 |
| 5 | GET | /api/employee/{id} | ○ | F-EMP-06 | 社員詳細取得 |
| 6 | PUT | /api/employee/{id} | ○ | F-EMP-04 | 社員情報更新 |
| 7 | DELETE | /api/employee/{id} | ○ | F-EMP-05 | 社員削除 |
| 8 | POST | /api/employee/{id}/upload | ○ | F-EMP-07 | 添付ファイルアップロード |
| 9 | GET | /api/employee/{id}/download | ○ | F-EMP-08 | 添付ファイルダウンロード |
| 10 | GET | /api/customer | ○ | F-CUS-01/02 | 顧客一覧取得・検索 |
| 11 | POST | /api/customer | ○ | F-CUS-03 | 顧客新規登録 |
| 12 | GET | /api/customer/{id} | ○ | F-CUS-06 | 顧客詳細取得 |
| 13 | PUT | /api/customer/{id} | ○ | F-CUS-04 | 顧客情報更新 |
| 14 | DELETE | /api/customer/{id} | ○ | F-CUS-05 | 顧客削除 |
| 15 | GET | /api/bank-accounts | ○ | F-CUS-07 | 銀行口座一覧取得（主テーブル） |
| 16 | POST | /api/bank-accounts | ○ | F-CUS-08 | 銀行口座追加 |
| 17 | GET | /api/bank-accounts/{id} | ○ | F-CUS-06 | 銀行口座詳細取得（紐付く顧客情報含む） |
| 18 | PUT | /api/bank-accounts/{id} | ○ | F-CUS-09 | 銀行口座更新 |
| 19 | DELETE | /api/bank-accounts/{id} | ○ | F-CUS-10 | 銀行口座削除 |
| 20 | GET | /api/attendance | ○ | F-ATT-01/02 | 勤務一覧取得・絞込 |
| 21 | POST | /api/attendance | ○ | F-ATT-03 | 勤務新規登録 |
| 22 | PUT | /api/attendance/{id} | ○ | F-ATT-04 | 勤務情報更新 |
| 23 | DELETE | /api/attendance/{id} | ○ | F-ATT-05 | 勤務削除 |
| 24 | GET | /api/attendance/export | ○ | F-ATT-06 | CSVエクスポート |
| 25 | GET | /api/invoice | ○ | F-INV-01/02 | 請求書一覧取得・絞込 |
| 26 | POST | /api/invoice | ○ | F-INV-03 | 請求書新規登録 |
| 27 | GET | /api/invoice/{id} | ○ | F-INV-06 | 請求書詳細取得 (含注文書) |
| 28 | PUT | /api/invoice/{id} | ○ | F-INV-04 | 請求書情報更新 |
| 29 | DELETE | /api/invoice/{id} | ○ | F-INV-05 | 請求書削除 |
| 30 | POST | /api/invoice/{id}/documents | ○ | F-INV-07 | 注文書アップロード |
| 31 | GET | /api/invoice/{id}/documents/{docId}/download | ○ | F-INV-08 | 注文書ダウンロード |
| 32 | DELETE | /api/invoice/{id}/documents/{docId} | ○ | F-INV-09 | 注文書削除 |

---

## 2. 各API詳細仕様

### 2.1 認証 API

#### POST /api/auth/login

**Request**:
```http
POST /api/auth/login HTTP/1.1
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Success Response (200)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTY4NTAwMDAwMCwiZXhwIjoxNjg1MDg2NDAwfQ.signature",
  "type": "Bearer",
  "username": "admin",
  "role": "ADMIN"
}
```

**Error Response (401)**:
```json
{
  "error": "ユーザー名またはパスワードが間違っています。",
  "status": 401,
  "timestamp": "2026-05-27T10:30:00",
  "path": "/api/auth/login"
}
```

#### GET /api/auth/me

**Request**:
```http
GET /api/auth/me HTTP/1.1
Authorization: Bearer {jwt_token}
```

**Success Response (200)**:
```json
{
  "data": {
    "username": "admin",
    "role": "ADMIN"
  },
  "message": "success"
}
```

---

### 2.2 社員管理 API

#### GET /api/employee

**Query Parameters**:

| Param | Type | Required | Default | Description |
|-------|------|:--------:|---------|-------------|
| keyword | String | - | null | 社員番号/氏名 部分一致 |
| department | String | - | null | 部署フィルター |
| page | int | - | 0 | ページ番号 (0-based) |
| size | int | - | 10 | ページサイズ |

**Request Example**:
```http
GET /api/employee?keyword=山田&department=営業部&page=0&size=10
Authorization: Bearer {jwt_token}
```

**Success Response (200)**:
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "employeeCode": "EMP001",
        "name": "山田 太郎",
        "email": "yamada@example.com",
        "phone": "090-1111-2222",
        "address": "東京都新宿区西新宿1-1-1",
        "department": "営業部",
        "position": "課長",
        "bankName": "三菱UFJ銀行",
        "bankBranch": "新宿支店",
        "bankAccountType": "普通",
        "bankAccountNumber": "1234567",
        "bankAccountHolder": "ヤマダ タロウ",
        "attachmentPath": "/uploads/employee/abc123.pdf",
        "createTime": "2026-05-01T10:00:00",
        "updateTime": "2026-05-27T09:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 10,
    "first": true,
    "last": true,
    "empty": false
  },
  "message": "success"
}
```

#### POST /api/employee

**Request**:
```http
POST /api/employee HTTP/1.1
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "employeeCode": "EMP006",
  "name": "佐々木 健太",
  "email": "sasaki@example.com",
  "phone": "090-1234-5678",
  "address": "東京都目黒区自由が丘1-2-3",
  "department": "技術部",
  "position": "一般社員",
  "bankName": "三菱UFJ銀行",
  "bankBranch": "目黒支店",
  "bankAccountType": "普通",
  "bankAccountNumber": "1112223",
  "bankAccountHolder": "ササキ ケンタ"
}
```

**Success Response (201)**:
```json
{
  "data": {
    "id": 6,
    "employeeCode": "EMP006",
    "name": "佐々木 健太",
    ...
    "createTime": "2026-05-27T10:30:00",
    "updateTime": "2026-05-27T10:30:00"
  },
  "message": "社員を登録しました"
}
```

**Error Response (400)**:
```json
{
  "error": "この社員番号は既に使用されています",
  "status": 400,
  "timestamp": "2026-05-27T10:30:00",
  "path": "/api/employee"
}
```

#### PUT /api/employee/{id}

```http
PUT /api/employee/1 HTTP/1.1
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "employeeCode": "EMP001",
  "name": "山田 太郎",
  "department": "営業部",
  "position": "部長",
  ...
}
```

**Response**: 200 OK (更新後のEmployeeDTO)

#### DELETE /api/employee/{id}

```http
DELETE /api/employee/1 HTTP/1.1
Authorization: Bearer {jwt_token}
```

**Success Response (200)**:
```json
{
  "data": null,
  "message": "社員を削除しました"
}
```

**Error Response (404)**:
```json
{
  "error": "社員が見つかりません: 999",
  "status": 404,
  "timestamp": "2026-05-27T10:30:00",
  "path": "/api/employee/999"
}
```

#### POST /api/employee/{id}/upload

```http
POST /api/employee/1/upload HTTP/1.1
Authorization: Bearer {jwt_token}
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="resume.pdf"
Content-Type: application/pdf

(binary data)
------WebKitFormBoundary--
```

**Success Response (200)**:
```json
{
  "data": {
    "attachmentPath": "employee/uuid-resume.pdf",
    "originalFileName": "resume.pdf"
  },
  "message": "ファイルをアップロードしました"
}
```

#### GET /api/employee/{id}/download

```http
GET /api/employee/1/download HTTP/1.1
Authorization: Bearer {jwt_token}
```

**Response**: 200 OK, Content-Type: application/octet-stream, Content-Disposition: attachment

---

### 2.3 銀行口座管理 API（主テーブル）

#### GET /api/bank-accounts — 銀行口座一覧取得 ★機密API

| Param | Type | Required | Description |
|-------|------|:--------:|-------------|
| customerId | Long | - | 顧客IDで絞込（業務テーブル参照） |
| page | int | - | ページ番号 (0-based) |
| size | int | - | ページサイズ (default 10) |

**Response (200)**:
```json
{
  "data": {
    "content": [
      { "id": 1, "bankName": "みずほ銀行", "bankBranch": "東京中央支店", "bankAccountType": "普通", "bankAccountNumber": "7654321", "bankAccountHolder": "カ）テクノトウキョウ", "customerId": 1, "customerName": "株式会社テクノ東京" },
      { "id": 2, "bankName": "三菱UFJ銀行", "bankBranch": "丸の内支店", "bankAccountType": "当座", "bankAccountNumber": "1122334", "bankAccountHolder": "カ）テクノトウキョウ", "customerId": 1, "customerName": "株式会社テクノ東京" }
    ],
    "totalElements": 2
  }
}
```

#### GET /api/bank-accounts/{id} — 銀行口座詳細取得（紐付く顧客業務情報含む）★機密API

| 項目 | 内容 |
|------|------|
| 説明 | 銀行口座の詳細情報と、紐付く顧客（業務テーブル）の情報を一緒に返却 |

**Response (200)**:
```json
{
  "data": {
    "id": 1,
    "bankName": "みずほ銀行",
    "bankBranch": "東京中央支店",
    "bankAccountType": "普通",
    "bankAccountNumber": "7654321",
    "bankAccountHolder": "カ）テクノトウキョウ",
    "customer": {
      "id": 1,
      "customerCode": "CUS001",
      "companyName": "株式会社テクノ東京",
      "presidentName": "田中 宏",
      "website": "www.techno-tokyo.co.jp",
      "salesRepName": "中村 博",
      "adminRepName": "斉藤 雅子"
    }
  }
}
```

#### POST /api/bank-accounts — 銀行口座追加 ★機密API

Request body:
```json
{
  "customerId": 1,
  "bankName": "三井住友銀行",
  "bankBranch": "銀座支店",
  "bankAccountType": "普通",
  "bankAccountNumber": "9988776",
  "bankAccountHolder": "カ）テクノトウキョウ"
}
```

#### PUT /api/bank-accounts/{id} — 銀行口座更新 ★機密API

#### DELETE /api/bank-accounts/{id} — 銀行口座削除 ★機密API

---

### 2.4 顧客管理 API（業務テーブル）

#### GET /api/customer

| Param | Type | Required | Description |
|-------|------|:--------:|-------------|
| keyword | String | - | 顧客番号/会社名 部分一致 |
| page | int | - | ページ番号 (0-based) |
| size | int | - | ページサイズ (default 10) |

**Response (200)**: 顧客DTOのリスト（customerCode, companyName, presidentName, website, salesRepName, salesRepPhone, salesRepEmail, adminRepName, adminRepPhone, adminRepEmail, address 等）

#### POST /api/customer

Request body: 顧客DTO（customerCode*, companyName*, presidentName, website, contactName, email, phone, address, salesRepName, salesRepPhone, salesRepEmail, adminRepName, adminRepPhone, adminRepEmail）

---

### 2.5 勤務管理 API

#### GET /api/attendance

| Param | Type | Required | Description |
|-------|------|:--------:|-------------|
| year | int | - | 年度 (例: 2026) |
| month | int | - | 月度 (1-12) |
| employeeId | Long | - | 社員ID (省略=全社員) |
| page | int | - | ページ番号 |
| size | int | - | ページサイズ |

**Response (200)**:
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "employeeId": 1,
        "employeeName": "山田 太郎",
        "workDate": "2026-05-01",
        "workHours": 8.0,
        "overtimeHours": 0.0,
        "status": "出勤",
        "remark": null
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0,
    "size": 10
  },
  "message": "success"
}
```

#### POST /api/attendance

```http
POST /api/attendance HTTP/1.1
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "employeeId": 1,
  "workDate": "2026-05-10",
  "workHours": 8.0,
  "overtimeHours": 1.5,
  "status": "出勤",
  "remark": "プロジェクト会議参加"
}
```

**Validation Error (400)** — 重複チェック:
```json
{
  "error": "この日付の勤務記録は既に存在します",
  "status": 400
}
```

#### GET /api/attendance/export

| Param | Type | Required | Description |
|-------|------|:--------:|-------------|
| year | int | - | 年度 |
| month | int | - | 月度 |
| employeeId | Long | - | 社員ID |

**Response (200)**: Content-Type: text/csv; charset=UTF-8, ファイル添付ダウンロード

---

### 2.6 請求書・注文書管理 API

#### GET /api/invoice

| Param | Type | Required | Description |
|-------|------|:--------:|-------------|
| year | int | - | 年度 |
| month | int | - | 月度 |
| customerId | Long | - | 顧客ID |
| page | int | - | ページ番号 |
| size | int | - | ページサイズ |

**Response (200)**:
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "invoiceNumber": "INV-2026-0501",
        "customerId": 1,
        "customerName": "株式会社テクノ東京",
        "year": 2026,
        "month": 5,
        "amount": 1500000.00,
        "status": "下書き",
        "remark": "システム開発費用",
        "createTime": "2026-05-01T09:00:00"
      }
    ],
    "totalElements": 3,
    "totalPages": 1
  },
  "message": "success"
}
```

#### GET /api/invoice/{id}

**詳細取得 (注文書一覧を含む)**:

```json
{
  "data": {
    "id": 1,
    "invoiceNumber": "INV-2026-0501",
    "customerId": 1,
    "customerName": "株式会社テクノ東京",
    "year": 2026,
    "month": 5,
    "amount": 1500000.00,
    "status": "下書き",
    "remark": "システム開発費用",
    "documents": [
      {
        "id": 1,
        "fileName": "注文書_20260501.pdf",
        "filePath": "invoices/uuid-abc.pdf",
        "fileSize": 245760,
        "createTime": "2026-05-01T10:30:00"
      },
      {
        "id": 2,
        "fileName": "仕様書_v2.xlsx",
        "filePath": "invoices/uuid-def.xlsx",
        "fileSize": 184320,
        "createTime": "2026-05-03T14:15:00"
      }
    ]
  },
  "message": "success"
}
```

#### DELETE /api/invoice/{id}

**Response (200)**:
```json
{
  "data": null,
  "message": "請求書を削除しました（紐付く注文書も削除されました）"
}
```

#### POST /api/invoice/{id}/documents

multipart/form-data でファイルアップロード。Employeeのupload と同様。

#### GET /api/invoice/{id}/documents/{docId}/download

**Response**: 200 OK, application/octet-stream, Content-Disposition: attachment

#### DELETE /api/invoice/{id}/documents/{docId}

```http
DELETE /api/invoice/1/documents/2 HTTP/1.1
Authorization: Bearer {jwt_token}
```

**Response (200)**:
```json
{
  "data": null,
  "message": "注文書を削除しました"
}
```

---

## 3. 共通HTTPレスポンスパターン

### 3.1 成功レスポンス

| HTTP Status | 意味 | 使用API |
|:----------:|------|--------|
| 200 OK | GET/PUT/DELETE 成功 | 一覧、詳細、更新、削除 |
| 201 Created | POST 作成成功 | 新規登録系 |

### 3.2 エラーレスポンス

```json
{
  "error": "人間可読なエラーメッセージ",
  "status": 400,
  "timestamp": "2026-05-27T10:30:00",
  "path": "/api/employee/999",
  "details": ["フィールド名: 詳細メッセージ"]
}
```

### 3.3 バリデーションエラー詳細

```json
{
  "error": "バリデーションエラー",
  "status": 400,
  "timestamp": "2026-05-27T10:30:00",
  "path": "/api/employee",
  "details": [
    "employeeCode: 社員番号は必須です",
    "email: メールアドレスの形式が正しくありません"
  ]
}
```

---

## 4. APIテスト仕様

### 4.1 テスト用 curl コマンド集

```bash
# ベースURL
BASE="http://localhost:8080/api"

# 1. ログイン (トークン取得)
TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# 2. 社員一覧
curl -s "$BASE/employee?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq

# 3. 社員新規登録
curl -s -X POST "$BASE/employee" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"employeeCode":"EMP007","name":"テスト 太郎"}' | jq

# 4. 勤務一覧 (2026年5月)
curl -s "$BASE/attendance?year=2026&month=5" \
  -H "Authorization: Bearer $TOKEN" | jq

# 5. CSVエクスポート
curl -s "$BASE/attendance/export?year=2026&month=5" \
  -H "Authorization: Bearer $TOKEN" \
  -o attendance_202605.csv

# 6. ファイルアップロード
curl -s -X POST "$BASE/employee/1/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/document.pdf" | jq
```

### 4.2 テストケース一覧

| # | API | テスト内容 | 期待結果 |
|:--|-----|-----------|---------|
| T01 | POST /auth/login | 正しい認証情報 | 200, token返却 |
| T02 | POST /auth/login | 誤ったパスワード | 401, エラーメッセージ |
| T03 | POST /auth/login | 空のユーザー名 | 400, バリデーションエラー |
| T04 | GET /auth/me | 有効トークン | 200, ユーザー情報 |
| T05 | GET /auth/me | 無効トークン | 401 |
| T06 | GET /employee | ページネーション | 200, 10件以内 |
| T07 | GET /employee?keyword=山田 | 部分一致検索 | 200, 該当データのみ |
| T08 | POST /employee | 必須項目のみ | 201 |
| T09 | POST /employee | employeeCode重複 | 400 |
| T10 | POST /employee | バリデーションエラー | 400, detailsあり |
| T11 | GET /employee/{id} | 存在するID | 200 |
| T12 | GET /employee/{id} | 存在しないID | 404 |
| T13 | PUT /employee/{id} | 部署変更 | 200, 更新後データ |
| T14 | DELETE /employee/{id} | 存在するID | 200 |
| T15 | POST /employee/{id}/upload | PDFファイル | 200 |
| T16 | POST /attendance | 重複日付 | 400, 重複エラー |
| T17 | GET /attendance/export | CSV出力 | 200, text/csv |
| T18 | DELETE /invoice/{id} | 請求書削除＋注文書CASCADE | 200 |
