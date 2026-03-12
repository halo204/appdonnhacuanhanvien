# 🚀 ÁP DỤNG FIXES NGAY - HƯỚNG DẪN NHANH

## ⏱️ Thời gian: 30 phút

---

## BƯỚC 1: CẬP NHẬT DATABASE (5 phút)

### 1.1. Mở phpMyAdmin
```
http://localhost/phpmyadmin
```

### 1.2. Chọn database `cleaning_service`

### 1.3. Click tab "SQL"

### 1.4. Copy toàn bộ nội dung file này và paste vào:
```
appdonnhacuanhanvien/BACKEND_FIXES.sql
```

### 1.5. Click "Go" để chạy

✅ **Kết quả**: Database đã có các cột và bảng mới

---

## BƯỚC 2: BACKUP API SERVER (1 phút)

### 2.1. Mở terminal tại thư mục `appdonnhacuanhanvien`

### 2.2. Chạy lệnh:
```bash
cp api-server-example.js api-server-example.backup.js
```

✅ **Kết quả**: Đã có file backup để restore nếu cần

---

## BƯỚC 3: CẬP NHẬT API SERVER (20 phút)

### 3.1. Mở file `api-server-example.js`

### 3.2. Tìm và THAY THẾ function `POST /api/bookings`

**Tìm dòng:**
```javascript
// Create booking
app.post('/api/bookings', async (req, res) => {
```

**Thay bằng code từ:**
```
CRITICAL_BACKEND_FIXES.md → Mục 1
```

---

### 3.3. THÊM MỚI function `PUT /api/bookings/:id/accept`

**Thêm sau function `POST /api/bookings`:**

**Copy code từ:**
```
CRITICAL_BACKEND_FIXES.md → Mục 2
```

---

### 3.4. Tìm và THAY THẾ function `PUT /api/bookings/:id/status`

**Tìm dòng:**
```javascript
// Update booking status
app.put('/api/bookings/:id/status', async (req, res) => {
```

**Thay bằng code từ:**
```
CRITICAL_BACKEND_FIXES.md → Mục 3
```

---

### 3.5. Tìm và THAY THẾ function `POST /api/payments`

**Tìm dòng:**
```javascript
// Create payment
app.post('/api/payments', async (req, res) => {
```

**Thay bằng code từ:**
```
CRITICAL_BACKEND_FIXES.md → Mục 4
```

---

### 3.6. Tìm và THAY THẾ function `POST /api/reviews`

**Tìm dòng:**
```javascript
// Create review
app.post('/api/reviews', async (req, res) => {
```

**Thay bằng code từ:**
```
CRITICAL_BACKEND_FIXES.md → Mục 5
```

---

### 3.7. THÊM MỚI function `PUT /api/bookings/:id/cancel`

**Thêm sau function `POST /api/reviews`:**

**Copy code từ:**
```
CRITICAL_BACKEND_FIXES.md → Mục 6
```

---

## BƯỚC 4: RESTART API SERVER (1 phút)

### 4.1. Stop server hiện tại
```
Ctrl + C trong terminal đang chạy server
```

### 4.2. Start lại server
```bash
node api-server-example.js
```

### 4.3. Kiểm tra log
```
🚀 API Server running on http://localhost:3000
📊 Database: cleaning_service
```

✅ **Kết quả**: Server đã chạy với logic mới

---

## BƯỚC 5: TEST FLOW MỚI (3 phút)

### 5.1. Test trên Customer App

1. **Đặt đơn mới**
   - Chọn dịch vụ
   - Chọn worker
   - Đặt đơn
   - ✅ Status = PENDING

2. **Kiểm tra database**
   ```sql
   SELECT * FROM bookings ORDER BY created_at DESC LIMIT 1;
   ```
   - ✅ status = 'PENDING'
   - ✅ worker_accepted_at = NULL

3. **Worker accept** (test bằng Postman hoặc admin panel)
   ```
   PUT http://localhost:3000/api/bookings/:id/accept
   Body: { "workerId": "worker_xxx" }
   ```
   - ✅ Status = WORKER_ASSIGNED

4. **Update status**
   ```
   PUT http://localhost:3000/api/bookings/:id/status
   Body: { "status": "WORKER_ON_WAY", "changedBy": "worker_xxx" }
   ```
   - ✅ Status = WORKER_ON_WAY

5. **Complete**
   ```
   PUT http://localhost:3000/api/bookings/:id/status
   Body: { "status": "COMPLETED", "changedBy": "worker_xxx" }
   ```
   - ✅ Status = COMPLETED
   - ✅ Worker is_available = TRUE

6. **Payment** (trên Customer App)
   - Chọn phương thức thanh toán
   - ✅ Payment created AFTER completed

7. **Review** (trên Customer App)
   - Đánh giá worker
   - ✅ Review created
   - ✅ Worker rating updated

---

## ❌ NẾU GẶP LỖI

### Lỗi: "Column 'is_available' doesn't exist"
**Nguyên nhân**: Chưa chạy BACKEND_FIXES.sql

**Giải pháp**: Quay lại Bước 1

---

### Lỗi: "Cannot read property 'status' of undefined"
**Nguyên nhân**: Thiếu parameter trong request

**Giải pháp**: Kiểm tra request body có đầy đủ fields

---

### Lỗi: Server không start
**Nguyên nhân**: Syntax error trong code

**Giải pháp**: 
```bash
# Restore backup
cp api-server-example.backup.js api-server-example.js
# Thử lại từ Bước 3
```

---

## ✅ CHECKLIST HOÀN THÀNH

- [ ] Database đã có cột `is_available`, `current_booking_id`
- [ ] Database đã có bảng `booking_status_history`
- [ ] Database đã có bảng `cancellation_fees`
- [ ] API server đã backup
- [ ] Function `POST /api/bookings` đã thay thế
- [ ] Function `PUT /api/bookings/:id/accept` đã thêm
- [ ] Function `PUT /api/bookings/:id/status` đã thay thế
- [ ] Function `POST /api/payments` đã thay thế
- [ ] Function `POST /api/reviews` đã thay thế
- [ ] Function `PUT /api/bookings/:id/cancel` đã thêm
- [ ] Server restart thành công
- [ ] Test flow mới thành công

---

## 🎉 HOÀN THÀNH!

Hệ thống đã có logic giống Grab/Uber/Gojek:
- ✅ Booking status flow đầy đủ
- ✅ Worker availability management
- ✅ Payment sau khi completed
- ✅ Review validation chặt chẽ
- ✅ Cancellation với phí hủy
- ✅ Transaction safety

**Next steps**:
- Cập nhật Android app để hiển thị các status mới
- Thêm notification system
- Thêm GPS tracking
- Tích hợp payment gateway thật (MoMo, VNPay)
