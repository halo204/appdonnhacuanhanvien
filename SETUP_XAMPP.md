# 🚀 Setup Database với XAMPP (Windows)

## Bước 1: Download và cài đặt XAMPP

1. Download XAMPP từ: https://www.apachefriends.org/download.html
2. Chọn phiên bản Windows
3. Cài đặt (mặc định vào `C:\xampp`)

## Bước 2: Khởi động MySQL

1. Mở **XAMPP Control Panel**
2. Click **Start** ở dòng **MySQL**
3. Đợi đến khi hiện chữ "Running" màu xanh

## Bước 3: Mở phpMyAdmin

1. Trong XAMPP Control Panel, click **Admin** ở dòng MySQL
2. Hoặc mở browser: http://localhost/phpmyadmin

## Bước 4: Import Database

### Cách 1: Dùng phpMyAdmin (Đơn giản)

1. Trong phpMyAdmin, click tab **SQL** ở trên
2. Copy toàn bộ nội dung file `database-schema.sql`
3. Paste vào ô SQL
4. Click **Go** (hoặc **Thực hiện**)
5. Đợi import xong
6. Lặp lại với file `sample-data.sql`

### Cách 2: Dùng Command Line

```bash
# Mở Command Prompt hoặc PowerShell
cd C:\xampp\mysql\bin

# Import database
.\mysql.exe -u root -p < E:\appdonnhacuanhanvien\database-schema.sql
# Nhấn Enter (không cần password nếu mới cài)

# Import sample data
.\mysql.exe -u root -p < E:\appdonnhacuanhanvien\sample-data.sql
```

## Bước 5: Kiểm tra Database

1. Trong phpMyAdmin, click **cleaning_service** ở sidebar trái
2. Bạn sẽ thấy 11 tables:
   - users
   - workers
   - services
   - bookings
   - reviews
   - addresses
   - booking_updates
   - worker_locations
   - earnings_history
   - notifications
   - service_categories

## Bước 6: Setup API Server

```bash
# Mở PowerShell tại thư mục appdonnhacuanhanvien
cd E:\appdonnhacuanhanvien

# Cài đặt Node.js dependencies
npm install

# Tạo file .env
copy .env.example .env

# Sửa file .env:
# DB_HOST=localhost
# DB_USER=root
# DB_PASSWORD=           (để trống nếu XAMPP mặc định)
# DB_NAME=cleaning_service
# PORT=3000
# JWT_SECRET=my_secret_key_123

# Chạy server
npm start
```

## Bước 7: Test API

Mở browser hoặc Postman:

```
http://localhost:3000/api/services
```

Bạn sẽ thấy danh sách dịch vụ!

## ⚠️ Lưu ý

- XAMPP MySQL mặc định không có password cho user `root`
- Nếu muốn set password:
  1. Mở phpMyAdmin
  2. Click **User accounts**
  3. Click **Edit privileges** cho user `root`
  4. Click **Change password**

## 🐛 Troubleshooting

### MySQL không start được:
- Port 3306 bị chiếm → Đổi port trong XAMPP Config
- Hoặc tắt MySQL service khác đang chạy

### phpMyAdmin không mở được:
- Apache chưa start → Start Apache trong XAMPP
- Port 80 bị chiếm → Đổi port Apache

### Import SQL bị lỗi:
- Chọn từng table import riêng
- Hoặc tăng `max_allowed_packet` trong my.ini

---

**Xong!** Database đã sẵn sàng với XAMPP.
