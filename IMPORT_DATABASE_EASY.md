# 🎯 Import Database - Cách Đơn Giản Nhất

## Bước 1: Cài XAMPP

1. Download: https://www.apachefriends.org/download.html
2. Install (Next, Next, Finish)
3. Mở XAMPP Control Panel
4. Click **Start** ở dòng **MySQL**
5. Click **Start** ở dòng **Apache**

## Bước 2: Mở phpMyAdmin

1. Mở browser
2. Vào: `http://localhost/phpmyadmin`

## Bước 3: Import Database

### 3.1. Tạo Database

1. Click tab **SQL** (ở trên cùng)
2. Copy và paste dòng này:

```sql
CREATE DATABASE IF NOT EXISTS cleaning_service;
```

3. Click **Go**
4. Thấy "Query OK" → Thành công!

### 3.2. Import Tables

**File database-schema.sql đang ở:** `E:\database-schema.sql`

1. Click vào **cleaning_service** ở sidebar trái
2. Click tab **Import**
3. Click **Choose File**
4. Chọn file: `E:\database-schema.sql`
5. Scroll xuống dưới
6. Click **Go**
7. Đợi 10-30 giây
8. Thấy "Import has been successfully finished" → Thành công!

### 3.3. Import Sample Data

**File sample-data.sql đang ở:** `E:\appdonnhacuanhanvien\sample-data.sql`

1. Vẫn ở database **cleaning_service**
2. Click tab **Import**
3. Click **Choose File**
4. Chọn file: `E:\appdonnhacuanhanvien\sample-data.sql`
5. Click **Go**
6. Đợi import xong

## Bước 4: Kiểm tra

1. Click vào **cleaning_service** ở sidebar
2. Bạn sẽ thấy 11 tables:
   - ✅ addresses
   - ✅ booking_updates
   - ✅ bookings
   - ✅ earnings_history
   - ✅ notifications
   - ✅ review_photos
   - ✅ reviews
   - ✅ service_categories
   - ✅ services
   - ✅ users
   - ✅ worker_locations
   - ✅ workers

3. Click vào table **services**
4. Click tab **Browse**
5. Bạn sẽ thấy 5 dịch vụ mẫu → OK!

## Bước 5: Chạy API Server

```powershell
# Mở PowerShell
cd E:\appdonnhacuanhanvien

# Chạy server
npm start
```

Bạn sẽ thấy:
```
🚀 API Server running on http://localhost:3000
📊 Database: cleaning_service
```

## Bước 6: Test API

Mở browser: `http://localhost:3000/api/services`

Bạn sẽ thấy JSON với danh sách dịch vụ!

```json
{
  "success": true,
  "services": [
    {
      "id": "svc_1",
      "name": "Vệ sinh nhà cửa tổng quát",
      "price": 200000,
      ...
    }
  ]
}
```

## ✅ Xong!

Database đã sẵn sàng. Bây giờ có thể:
1. Build Worker App
2. Build Customer App
3. Test toàn bộ hệ thống

---

## 🐛 Nếu gặp lỗi

### MySQL không start:
- Tắt MySQL service khác đang chạy
- Hoặc đổi port trong XAMPP Config

### phpMyAdmin không mở:
- Check Apache đã start chưa
- Thử: `http://127.0.0.1/phpmyadmin`

### Import bị lỗi:
- File quá lớn → Tăng `upload_max_filesize` trong php.ini
- Hoặc copy-paste từng phần SQL

### API không chạy:
- Check file `.env` đã có chưa
- Check MySQL đang chạy
- Check port 3000 không bị chiếm

---

**Vị trí các file quan trọng:**

- Database schema: `E:\database-schema.sql`
- Sample data: `E:\appdonnhacuanhanvien\sample-data.sql`
- API server: `E:\appdonnhacuanhanvien\api-server-example.js`
- Config: `E:\appdonnhacuanhanvien\.env`
