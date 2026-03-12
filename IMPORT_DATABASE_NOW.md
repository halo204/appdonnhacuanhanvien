# 📥 IMPORT DATABASE - HƯỚNG DẪN NHANH

## Vấn Đề
App đăng nhập thành công nhưng không có dữ liệu dịch vụ hiển thị.

## Nguyên Nhân
Database chưa có dữ liệu hoặc chưa được import đầy đủ.

## Giải Pháp - Import Database Đầy Đủ

### Cách 1: Qua phpMyAdmin (Khuyến nghị)

#### Bước 1: Mở phpMyAdmin
1. Mở trình duyệt
2. Truy cập: http://localhost/phpmyadmin
3. Đăng nhập (thường không cần password)

#### Bước 2: Import File SQL
1. Click tab "Import" ở menu trên
2. Click nút "Choose File"
3. Chọn file: `E:\appdonnhacuanhanvien\import-all-data.sql`
4. Scroll xuống dưới và click "Import"
5. Đợi vài giây

#### Bước 3: Kiểm Tra
Sau khi import thành công, bạn sẽ thấy:
- Database `cleaning_service` được tạo
- Các bảng: services, users, workers, bookings, etc.
- Thông báo: "Database imported successfully!"
- Số lượng: 6 services, 2 users, 4 workers

### Cách 2: Qua Command Line

Mở Command Prompt hoặc PowerShell và chạy:

```bash
# Đảm bảo MySQL đang chạy trong XAMPP
# Sau đó chạy lệnh sau:

mysql -u root < E:\appdonnhacuanhanvien\import-all-data.sql
```

Nếu có password cho MySQL:
```bash
mysql -u root -p < E:\appdonnhacuanhanvien\import-all-data.sql
```

### Cách 3: Copy-Paste SQL

1. Mở file `import-all-data.sql` trong editor
2. Copy toàn bộ nội dung
3. Mở phpMyAdmin: http://localhost/phpmyadmin
4. Click tab "SQL"
5. Paste nội dung vào
6. Click "Go"

## Sau Khi Import

### 1. Restart API Server (Nếu Cần)
```bash
# Stop server hiện tại (Ctrl+C)
# Sau đó start lại:
cd appdonnhacuanhanvien
node api-server-example.js
```

### 2. Test API
Mở trình duyệt và truy cập:
- http://localhost:3000/api/services

Bạn sẽ thấy danh sách 6 dịch vụ.

### 3. Test App
Mở app và:
1. Đăng nhập với:
   - Email: `customer1@example.com`
   - Password: `password123`

2. Vào tab "Dịch vụ" - Bạn sẽ thấy 6 dịch vụ:
   - 🏠 Vệ sinh nhà cửa tổng quát - 200,000đ
   - 🍳 Vệ sinh bếp - 150,000đ
   - 🚿 Vệ sinh phòng tắm - 120,000đ
   - 🪟 Lau kính cửa sổ - 100,000đ
   - 🛋️ Giặt thảm, sofa - 300,000đ
   - 👔 Giặt ủi quần áo - 50,000đ

## Dữ Liệu Có Sẵn Sau Import

### Tài Khoản Khách Hàng
- Email: `customer1@example.com` / Password: `password123`
- Email: `customer2@example.com` / Password: `password123`

### Tài Khoản Nhân Viên
- Email: `worker1@example.com` / Password: `password123` (Lê Văn C - Rating 4.8⭐)
- Email: `worker2@example.com` / Password: `password123` (Phạm Thị D - Rating 4.9⭐)
- Email: `worker3@example.com` / Password: `password123` (Hoàng Văn E - Rating 4.7⭐)
- Email: `worker4@example.com` / Password: `password123` (Võ Thị F - Rating 4.6⭐)

### Dịch Vụ
- 6 dịch vụ trong 3 categories
- Giá từ 50,000đ đến 300,000đ
- Thời gian từ 60 phút đến 180 phút

### Bookings
- 1 booking mẫu cho customer1

## Troubleshooting

### Lỗi "Database already exists"
Không sao! File SQL sẽ tự động xóa và tạo lại database mới.

### Lỗi "Access denied"
1. Kiểm tra MySQL đang chạy trong XAMPP
2. Thử đăng nhập phpMyAdmin với username: `root`, password: (để trống)

### App vẫn không có dữ liệu
1. Kiểm tra API server đang chạy: http://localhost:3000/api/services
2. Restart API server
3. Force close app và mở lại
4. Kiểm tra database có dữ liệu:
   ```sql
   USE cleaning_service;
   SELECT * FROM services;
   ```

### API trả về lỗi
1. Kiểm tra file `.env` có đúng cấu hình:
   ```
   DB_HOST=localhost
   DB_USER=root
   DB_PASSWORD=
   DB_NAME=cleaning_service
   ```
2. Restart API server

## Kiểm Tra Nhanh

Chạy lệnh này để kiểm tra database:

```sql
USE cleaning_service;
SELECT 'Services' as table_name, COUNT(*) as count FROM services
UNION ALL
SELECT 'Users', COUNT(*) FROM users
UNION ALL
SELECT 'Workers', COUNT(*) FROM workers
UNION ALL
SELECT 'Bookings', COUNT(*) FROM bookings;
```

Kết quả mong đợi:
- Services: 6
- Users: 2
- Workers: 4
- Bookings: 1

## Lưu Ý Quan Trọng

⚠️ File `import-all-data.sql` sẽ **XÓA** database cũ và tạo mới!
⚠️ Nếu bạn đã có dữ liệu quan trọng, hãy backup trước!
✅ Tất cả passwords đều là: `password123`
✅ Passwords đã được hash bằng bcrypt (bảo mật)
