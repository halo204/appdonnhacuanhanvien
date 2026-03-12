# 🎬 Hướng dẫn Setup Hoàn chỉnh - Từ A đến Z

## 📦 Chuẩn bị

### Cần cài đặt:
1. ✅ XAMPP - https://www.apachefriends.org/download.html
2. ✅ Node.js - https://nodejs.org/ (LTS version)
3. ✅ Android Studio (đã có)

---

## 🗄️ PHẦN 1: SETUP DATABASE (10 phút)

### Bước 1.1: Cài XAMPP
```
1. Download XAMPP
2. Chạy installer
3. Chọn MySQL, Apache, phpMyAdmin
4. Install vào C:\xampp
5. Finish
```

### Bước 1.2: Start MySQL
```
1. Mở XAMPP Control Panel
2. Click "Start" ở dòng MySQL
3. Đợi đến khi hiện "Running" màu xanh
4. (Optional) Click "Start" ở Apache nếu muốn dùng phpMyAdmin
```

### Bước 1.3: Import Database

**Cách 1: Dùng phpMyAdmin (Dễ nhất)**
```
1. Mở browser: http://localhost/phpmyadmin
2. Click tab "SQL"
3. Mở file: E:\appdonnhacuanhanvien\database-schema.sql
4. Copy toàn bộ nội dung
5. Paste vào ô SQL
6. Click "Go"
7. Đợi 10-30 giây
8. Thấy "Import successful" → OK!
9. Lặp lại với file sample-data.sql
```

**Cách 2: Dùng Command Line**
```powershell
# Mở PowerShell
cd C:\xampp\mysql\bin

# Import schema
.\mysql.exe -u root < E:\appdonnhacuanhanvien\database-schema.sql

# Import data
.\mysql.exe -u root < E:\appdonnhacuanhanvien\sample-data.sql
```

### Bước 1.4: Kiểm tra
```
1. Trong phpMyAdmin, click "cleaning_service" ở sidebar
2. Thấy 11 tables → Thành công!
3. Click table "services" → Browse
4. Thấy 5 dịch vụ → OK!
```

---

## 🌐 PHẦN 2: SETUP API SERVER (5 phút)

### Bước 2.1: Cài Node.js
```
1. Download từ https://nodejs.org/
2. Chọn LTS version
3. Install (Next, Next, Finish)
4. Restart PowerShell
```

### Bước 2.2: Install Dependencies
```powershell
# Mở PowerShell
cd E:\appdonnhacuanhanvien

# Install packages
npm install
```

### Bước 2.3: Cấu hình .env
```powershell
# Copy file mẫu
copy .env.example .env

# Mở file .env bằng Notepad
notepad .env
```

Sửa nội dung:
```env
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=
DB_NAME=cleaning_service
PORT=3000
JWT_SECRET=my_super_secret_key_123
```

Save và đóng.

### Bước 2.4: Chạy Server
```powershell
npm start
```

Bạn sẽ thấy:
```
🚀 API Server running on http://localhost:3000
📊 Database: cleaning_service
```

### Bước 2.5: Test API
```
Mở browser: http://localhost:3000/api/services

Bạn sẽ thấy JSON với danh sách dịch vụ!
```

---

## 📱 PHẦN 3: SETUP WORKER APP (5 phút)

### Bước 3.1: Kiểm tra BASE_URL

File: `appdonnhacuanhanvien/app/src/main/java/com/example/donvesinhcuanv/data/api/RetrofitClient.kt`

Đảm bảo có:
```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/"  // Cho Emulator
```

### Bước 3.2: Build App
```powershell
cd E:\appdonnhacuanhanvien
.\gradlew assembleDebug
```

### Bước 3.3: Run App
```
1. Mở Android Studio
2. Open project: E:\appdonnhacuanhanvien
3. Start Emulator
4. Click Run (Shift+F10)
```

### Bước 3.4: Test Login
```
Email: worker1@example.com
Password: password123

Click "Đăng nhập"
→ Vào được màn hình Home → Thành công!
```

---

## 📱 PHẦN 4: SETUP CUSTOMER APP (5 phút)

### Bước 4.1: Kiểm tra Customer App có dùng MySQL chưa

Nếu Customer App vẫn dùng Firebase, cần chuyển sang MySQL tương tự Worker App.

### Bước 4.2: Build và Run
```
1. Open project: E:\Cleaner
2. Start Emulator
3. Click Run
```

---

## 🧪 PHẦN 5: TEST TOÀN BỘ HỆ THỐNG (10 phút)

### Test Flow 1: Worker đăng ký và đăng nhập
```
1. Mở Worker App
2. Click "Đăng ký"
3. Nhập thông tin
4. Submit
5. Đăng nhập với tài khoản vừa tạo
6. Vào được Home screen → OK!
```

### Test Flow 2: Customer tạo đơn
```
1. Mở Customer App
2. Đăng nhập
3. Chọn dịch vụ
4. Đặt lịch
5. Tạo đơn
6. Check database:
   - Vào phpMyAdmin
   - Table bookings
   - Thấy đơn mới → OK!
```

### Test Flow 3: Worker nhận đơn
```
1. Mở Worker App
2. Bật trạng thái Online
3. Thấy đơn mới trong danh sách
4. Click "Nhận đơn"
5. Đơn chuyển sang "Đang thực hiện"
6. Check database:
   - Table bookings
   - status = 'WORKER_ASSIGNED' → OK!
```

### Test Flow 4: Worker hoàn thành đơn
```
1. Click "Đang đến"
2. Click "Bắt đầu"
3. Click "Hoàn thành"
4. Thu nhập tăng lên
5. Check database:
   - Table bookings: status = 'COMPLETED'
   - Table workers: completed_jobs tăng
   - Table earnings_history: có record mới
   → OK!
```

---

## ✅ CHECKLIST HOÀN THÀNH

### Database:
- [ ] XAMPP đã cài
- [ ] MySQL đang chạy
- [ ] Database cleaning_service đã tạo
- [ ] 11 tables đã import
- [ ] Sample data đã có

### API Server:
- [ ] Node.js đã cài
- [ ] npm install thành công
- [ ] File .env đã cấu hình
- [ ] Server chạy được (port 3000)
- [ ] Test API thành công

### Worker App:
- [ ] BASE_URL đã đúng
- [ ] Build thành công
- [ ] Đăng nhập được
- [ ] Nhận đơn được
- [ ] Hoàn thành đơn được

### Customer App:
- [ ] Build thành công
- [ ] Đăng nhập được
- [ ] Tạo đơn được
- [ ] Xem trạng thái đơn được

---

## 🎉 HOÀN THÀNH!

Hệ thống đã hoạt động hoàn chỉnh với:
- ✅ Database MySQL
- ✅ API Server
- ✅ Worker App
- ✅ Customer App

Cả 2 apps đã liên kết với nhau qua database!

---

## 📞 Troubleshooting

### API Server không chạy:
```
- Check MySQL đang chạy
- Check port 3000 không bị chiếm
- Check file .env đúng
```

### App không kết nối được API:
```
- Check BASE_URL đúng
- Check API server đang chạy
- Check firewall không chặn port 3000
```

### Database lỗi:
```
- Re-import database-schema.sql
- Check MySQL service đang chạy
- Check user/password đúng
```

---

**Chúc mừng!** Bạn đã setup thành công toàn bộ hệ thống!
