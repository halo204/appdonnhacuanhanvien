# ✅ Checklist Setup - Đánh dấu từng bước

## 📦 Chuẩn bị

- [ ] XAMPP đã download
- [ ] XAMPP đã cài đặt
- [ ] Node.js đã cài đặt (https://nodejs.org/)

---

## 🗄️ Database Setup

- [ ] XAMPP Control Panel đã mở
- [ ] MySQL đã Start (màu xanh "Running")
- [ ] Apache đã Start (màu xanh "Running")
- [ ] phpMyAdmin đã mở được (http://localhost/phpmyadmin)
- [ ] Database `cleaning_service` đã tạo
- [ ] File `database-schema.sql` đã import (11 tables)
- [ ] File `sample-data.sql` đã import (có dữ liệu mẫu)
- [ ] Kiểm tra table `services` có 5 dịch vụ

---

## 🌐 API Server Setup

- [ ] File `package.json` đã có trong `E:\appdonnhacuanhanvien\`
- [ ] Đã chạy `npm install` thành công
- [ ] File `.env` đã tạo và cấu hình:
  - [ ] DB_HOST=localhost
  - [ ] DB_USER=root
  - [ ] DB_PASSWORD= (để trống)
  - [ ] DB_NAME=cleaning_service
  - [ ] PORT=3000
- [ ] Đã chạy `npm start`
- [ ] Server đang chạy (thấy "API Server running")
- [ ] Test API thành công: http://localhost:3000/api/services

---

## 📱 Worker App Setup

- [ ] File `RetrofitClient.kt` có BASE_URL = "http://10.0.2.2:3000/"
- [ ] Build thành công (`.\gradlew assembleDebug`)
- [ ] App chạy được trên emulator
- [ ] Đăng nhập thành công với:
  - Email: worker1@example.com
  - Password: password123
- [ ] Vào được màn hình Home
- [ ] Bật được trạng thái Online
- [ ] Thấy danh sách đơn hàng (nếu có)

---

## 📱 Customer App Setup

- [ ] Customer App đã chuyển sang MySQL (nếu cần)
- [ ] Build thành công
- [ ] App chạy được
- [ ] Đăng nhập thành công
- [ ] Xem được danh sách dịch vụ
- [ ] Tạo được đơn hàng mới

---

## 🧪 Test Toàn Bộ Hệ Thống

### Test 1: Worker đăng ký mới
- [ ] Mở Worker App
- [ ] Click "Đăng ký"
- [ ] Nhập thông tin mới
- [ ] Đăng ký thành công
- [ ] Đăng nhập được với tài khoản mới

### Test 2: Customer tạo đơn
- [ ] Mở Customer App
- [ ] Đăng nhập
- [ ] Chọn dịch vụ
- [ ] Điền thông tin đặt lịch
- [ ] Tạo đơn thành công
- [ ] Kiểm tra trong phpMyAdmin → table `bookings` có đơn mới

### Test 3: Worker nhận đơn
- [ ] Mở Worker App
- [ ] Bật Online
- [ ] Thấy đơn mới trong danh sách
- [ ] Click "Nhận đơn"
- [ ] Đơn chuyển sang "Đang thực hiện"
- [ ] Kiểm tra phpMyAdmin → `bookings.status` = 'WORKER_ASSIGNED'

### Test 4: Worker hoàn thành đơn
- [ ] Click "Đang đến"
- [ ] Click "Bắt đầu"
- [ ] Click "Hoàn thành"
- [ ] Thu nhập tăng lên
- [ ] Kiểm tra phpMyAdmin:
  - [ ] `bookings.status` = 'COMPLETED'
  - [ ] `workers.completed_jobs` tăng
  - [ ] `workers.total_earnings` tăng
  - [ ] Table `earnings_history` có record mới

### Test 5: Customer xem trạng thái
- [ ] Mở Customer App
- [ ] Vào "Đơn hàng của tôi"
- [ ] Thấy đơn đã hoàn thành
- [ ] Trạng thái hiển thị đúng

---

## 🎉 Hoàn Thành!

Nếu tất cả đều ✅, hệ thống đã hoạt động hoàn chỉnh!

---

## 📞 Nếu có vấn đề

### API Server không chạy:
→ Xem file: `IMPORT_DATABASE_EASY.md`

### Database lỗi:
→ Xem file: `SETUP_XAMPP.md`

### App không kết nối:
→ Check BASE_URL và API server đang chạy

### Cần hướng dẫn chi tiết:
→ Xem file: `COMPLETE_SETUP_GUIDE.md`

---

**Vị trí các file:**

```
E:\
├── database-schema.sql          ← Import file này trước
└── appdonnhacuanhanvien\
    ├── sample-data.sql          ← Import file này sau
    ├── api-server-example.js    ← API server
    ├── package.json             ← Dependencies
    ├── .env                     ← Config
    └── app\                     ← Worker App source code
```
