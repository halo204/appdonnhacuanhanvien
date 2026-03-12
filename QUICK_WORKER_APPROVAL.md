# ⚡ DUYỆT NHÂN VIÊN - HƯỚNG DẪN NHANH

## 🎯 Mục Tiêu
Sau khi nhân viên đăng ký ở Worker App → Admin duyệt → Nhân viên xuất hiện trong Customer App

## 📋 Các Bước

### 1️⃣ Nhân Viên Đăng Ký (Worker App)
```
Mở Worker App → Đăng ký → Nhập thông tin → Hoàn tất
Status: PENDING (chờ duyệt)
```

### 2️⃣ Admin Duyệt
```
Mở: http://localhost:3000/admin-approve-workers.html
Click "✅ Duyệt" cho nhân viên mới
Status: APPROVED (đã duyệt)
```

### 3️⃣ Nhân Viên Xuất Hiện (Customer App)
```
Mở Customer App → Tab Dịch vụ → Chọn dịch vụ
→ Nhân viên mới xuất hiện trong danh sách
```

## 🚀 Khởi Động

### API Server
```bash
cd appdonnhacuanhanvien
node api-server-example.js
```

### Admin Panel
```
http://localhost:3000/admin-approve-workers.html
```

## 📊 Trạng Thái

| Status | Mô tả | Xuất hiện trong Customer App |
|--------|-------|------------------------------|
| PENDING | Chờ duyệt | ❌ Không |
| APPROVED | Đã duyệt | ✅ Có |
| REJECTED | Từ chối | ❌ Không |
| SUSPENDED | Tạm khóa | ❌ Không |

## 🔧 Duyệt Nhanh Qua SQL

Nếu muốn duyệt trực tiếp trong database:

```sql
-- Duyệt một nhân viên cụ thể
UPDATE workers SET status = 'APPROVED' WHERE email = 'test@example.com';

-- Duyệt tất cả nhân viên chờ duyệt
UPDATE workers SET status = 'APPROVED' WHERE status = 'PENDING';

-- Kiểm tra
SELECT id, name, email, status FROM workers;
```

## ✅ Kiểm Tra

### Test API
```bash
# Lấy tất cả nhân viên (admin)
curl http://localhost:3000/api/workers/all

# Lấy nhân viên đã duyệt (customer app)
curl http://localhost:3000/api/workers
```

### Test Database
```sql
USE cleaning_service;
SELECT name, email, status FROM workers ORDER BY created_at DESC;
```

## 🎉 Hoàn Tất!

Bây giờ khi nhân viên đăng ký mới:
1. Họ sẽ có status = PENDING
2. Admin duyệt qua Admin Panel
3. Status → APPROVED
4. Tự động xuất hiện trong Customer App
5. Khách hàng có thể chọn nhân viên đó

## 📞 Tài Khoản Test

### Admin Panel
- Không cần đăng nhập
- Truy cập trực tiếp: http://localhost:3000/admin-approve-workers.html

### Worker App
- Đăng ký tài khoản mới hoặc dùng:
- Email: `worker1@example.com`
- Password: `password123`

### Customer App
- Email: `customer1@example.com`
- Password: `password123`
