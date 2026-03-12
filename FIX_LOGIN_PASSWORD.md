# 🔧 FIX LỖI ĐĂNG NHẬP - PASSWORD KHÔNG ĐÚNG

## Vấn Đề
App báo lỗi "Đăng nhập thất bại: Unauthorized" vì mật khẩu trong database là hash giả, không khớp với password thật.

## Nguyên Nhân
- Database được import với hash giả: `$2b$10$abcdefghijklmnopqrstuvwxyz`
- API server dùng bcrypt để so sánh password
- Hash giả không match với password "password123"

## Giải Pháp

### Bước 1: Mở phpMyAdmin
1. Mở trình duyệt
2. Truy cập: http://localhost/phpmyadmin
3. Đăng nhập (thường không cần password)

### Bước 2: Chọn Database
1. Click vào database `cleaning_service` ở sidebar bên trái
2. Click tab "SQL" ở menu trên

### Bước 3: Chạy SQL Update
Copy và paste đoạn SQL sau vào ô SQL, sau đó click "Go":

```sql
USE cleaning_service;

-- Update users (customers) passwords
UPDATE users SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'customer1@example.com';
UPDATE users SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'customer2@example.com';

-- Update workers passwords
UPDATE workers SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'worker1@example.com';
UPDATE workers SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'worker2@example.com';
UPDATE workers SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'worker3@example.com';
UPDATE workers SET password = '$2b$10$S7.ZF96U4q7sdOAGdCDzr.7dcr.zVi6g6jpcSrEiskNNaw9s.gw2G' WHERE email = 'worker4@example.com';
```

### Bước 4: Kiểm Tra
Sau khi chạy SQL, bạn sẽ thấy thông báo:
- "6 rows affected" hoặc tương tự
- Có nghĩa là đã update thành công

### Bước 5: Test Đăng Nhập
Quay lại app và đăng nhập với:
- **Email**: `customer1@example.com`
- **Password**: `password123`

## Tài Khoản Test Sau Khi Fix

### Khách Hàng (Customer App)
- Email: `customer1@example.com` / Password: `password123`
- Email: `customer2@example.com` / Password: `password123`

### Nhân Viên (Worker App)
- Email: `worker1@example.com` / Password: `password123`
- Email: `worker2@example.com` / Password: `password123`
- Email: `worker3@example.com` / Password: `password123`
- Email: `worker4@example.com` / Password: `password123`

## Cách Khác: Dùng Command Line

Nếu bạn quen với command line:

```bash
# Chạy file SQL đã tạo sẵn
mysql -u root -p cleaning_service < appdonnhacuanhanvien/update-passwords.sql
```

## Kiểm Tra API Server

Test login qua curl:

```bash
curl -Method POST -Uri "http://localhost:3000/api/users/login" -ContentType "application/json" -Body '{"email":"customer1@example.com","password":"password123"}'
```

Nếu thành công, bạn sẽ thấy response có `"success":true` và `token`.

## Lưu Ý

- Hash này là hash thật của password "password123" được tạo bằng bcrypt
- Mỗi lần hash sẽ tạo ra hash khác nhau (do salt ngẫu nhiên)
- Nhưng tất cả đều match với password "password123"
- Đây là cách bcrypt hoạt động để bảo mật

## Troubleshooting

### Nếu vẫn lỗi sau khi update:
1. Kiểm tra API server có đang chạy: http://localhost:3000/api/services
2. Kiểm tra MySQL có đang chạy trong XAMPP
3. Kiểm tra database có tồn tại: `cleaning_service`
4. Restart API server nếu cần

### Nếu không thể mở phpMyAdmin:
1. Kiểm tra Apache có đang chạy trong XAMPP
2. Thử truy cập: http://127.0.0.1/phpmyadmin
3. Hoặc dùng command line như hướng dẫn ở trên
