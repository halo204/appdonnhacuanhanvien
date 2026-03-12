# 🚀 IMPORT SQL NGAY - 2 PHÚT

## Bước 1: Mở phpMyAdmin

Vào trình duyệt, mở:
```
http://localhost/phpmyadmin
```

---

## Bước 2: Chọn database

Click vào `cleaning_service` ở sidebar bên trái

---

## Bước 3: Click tab "SQL"

Ở menu trên cùng, click tab **SQL**

---

## Bước 4: Copy SQL

Mở file này trong VS Code:
```
appdonnhacuanhanvien/BACKEND_FIXES.sql
```

**Nhấn Ctrl+A** (chọn tất cả)
**Nhấn Ctrl+C** (copy)

---

## Bước 5: Paste và Run

1. Click vào ô SQL lớn trong phpMyAdmin
2. **Nhấn Ctrl+V** (paste)
3. Click nút **"Go"** ở góc dưới bên phải

---

## Bước 6: Kiểm tra kết quả

Nếu thành công, bạn sẽ thấy:
```
✓ Query OK, X rows affected
✓ Query OK, X rows affected
...
```

---

## Bước 7: Verify

Click vào bảng `workers` ở sidebar, click tab "Structure"

Bạn sẽ thấy 2 cột mới:
- `is_available` (BOOLEAN)
- `current_booking_id` (VARCHAR)

Click vào bảng `bookings`, bạn sẽ thấy:
- `cancelled_at` (DATETIME)
- `worker_accepted_at` (DATETIME)
- `worker_arrived_at` (DATETIME)
- `service_started_at` (DATETIME)

Và 2 bảng mới:
- `booking_status_history`
- `cancellation_fees`

---

## ✅ XONG!

Database đã được cập nhật!

**Bước tiếp theo**: Restart API server

```bash
# Trong terminal đang chạy server, nhấn Ctrl+C để stop

# Sau đó start lại:
node api-server-example.js
```

Xem log:
```
🚀 API Server running on http://localhost:3000
📊 Database: cleaning_service
```

---

## 🎉 HOÀN THÀNH!

Hệ thống đã sẵn sàng với logic mới!

Test bằng cách:
1. Mở Customer App
2. Đặt một đơn mới
3. Kiểm tra trong database: status = 'PENDING'
4. Worker available = FALSE

Tất cả đã hoạt động theo logic Grab/Uber/Gojek!
