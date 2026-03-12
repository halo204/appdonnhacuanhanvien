# 📝 Hướng dẫn Import Database - Từng bước đơn giản

## ✅ Yêu cầu

- XAMPP đã cài đặt
- MySQL đang chạy (màu xanh trong XAMPP Control Panel)

## 🎯 Bước 1: Mở phpMyAdmin

1. Mở browser
2. Vào: `http://localhost/phpmyadmin`
3. Bạn sẽ thấy giao diện phpMyAdmin

## 🎯 Bước 2: Tạo Database

1. Click tab **SQL** ở trên cùng
2. Copy và paste đoạn này:

```sql
CREATE DATABASE IF NOT EXISTS cleaning_service;
```

3. Click nút **Go** (hoặc **Thực hiện**)
4. Bạn sẽ thấy thông báo "Query OK"

## 🎯 Bước 3: Chọn Database

1. Click vào **cleaning_service** ở sidebar bên trái
2. Database này đang trống (chưa có tables)

## 🎯 Bước 4: Import Tables

### Cách A: Import toàn bộ file (Khuyến nghị)

1. Click tab **Import** ở trên
2. Click **Choose File**
3. Chọn file `database-schema.sql` từ thư mục `E:\appdonnhacuanhanvien\`
4. Click **Go** ở dưới cùng
5. Đợi 10-30 giây
6. Bạn sẽ thấy thông báo "Import has been successfully finished"

### Cách B: Copy-Paste từng phần (Nếu file quá lớn)

1. Mở file `database-schema.sql` bằng Notepad
2. Copy từ dòng `CREATE TABLE users` đến hết table đó
3. Paste vào tab SQL trong phpMyAdmin
4. Click Go
5. Lặp lại cho các tables khác

## 🎯 Bước 5: Import Sample Data

1. Click tab **Import**
2. Chọn file `sample-data.sql`
3. Click **Go**
4. Đợi import xong

## 🎯 Bước 6: Kiểm tra

1. Click vào **cleaning_service** ở sidebar
2. Bạn sẽ thấy 11 tables:
   - ✅ users
   - ✅ workers
   - ✅ services
   - ✅ bookings
   - ✅ reviews
   - ✅ addresses
   - ✅ booking_updates
   - ✅ worker_locations
   - ✅ earnings_history
   - ✅ notifications
   - ✅ service_categories

3. Click vào table **services**
4. Click tab **Browse**
5. Bạn sẽ thấy 5 dịch vụ mẫu

## 🎯 Bước 7: Test Query

1. Click tab **SQL**
2. Paste query này:

```sql
SELECT * FROM workers;
```

3. Click **Go**
4. Bạn sẽ thấy 3 nhân viên mẫu

## ✅ Xong!

Database đã sẵn sàng. Tiếp theo:

1. Setup API Server (xem file `QUICK_START.md`)
2. Chạy Worker App
3. Chạy Customer App

---

## 🐛 Nếu gặp lỗi

### Lỗi: "MySQL said: #1046 - No database selected"
→ Quay lại Bước 3, chọn database `cleaning_service`

### Lỗi: "Table already exists"
→ Drop table cũ trước:
```sql
DROP TABLE IF EXISTS table_name;
```

### Lỗi: "Max execution time exceeded"
→ Import từng table một (Cách B)

### Lỗi: "Cannot connect to MySQL"
→ Check XAMPP Control Panel, MySQL phải đang "Running"
