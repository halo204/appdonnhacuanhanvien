# 👨‍💼 HƯỚNG DẪN DUYỆT NHÂN VIÊN

## Tổng Quan
Khi nhân viên đăng ký tài khoản mới ở Worker App, họ sẽ có status = "PENDING" (chờ duyệt). Admin cần duyệt nhân viên trước khi họ xuất hiện trong Customer App.

## Quy Trình

### 1. Nhân Viên Đăng Ký (Worker App)
- Nhân viên mở Worker App
- Chọn "Đăng ký tài khoản"
- Nhập thông tin: Tên, Email, SĐT, Mật khẩu
- Nhấn "Đăng ký"
- ✅ Tài khoản được tạo với status = "PENDING"

### 2. Admin Duyệt (Admin Panel)
- Mở trình duyệt
- Truy cập: http://localhost:3000/admin-approve-workers.html
- Xem danh sách nhân viên chờ duyệt
- Click "✅ Duyệt" để chấp nhận
- Hoặc "❌ Từ chối" để từ chối

### 3. Nhân Viên Xuất Hiện (Customer App)
- Sau khi được duyệt (status = "APPROVED")
- Nhân viên tự động xuất hiện trong Customer App
- Khách hàng có thể chọn nhân viên khi đặt dịch vụ

## Trạng Thái Nhân Viên

### PENDING (Chờ duyệt)
- Nhân viên mới đăng ký
- Chưa xuất hiện trong Customer App
- Không thể nhận việc

### APPROVED (Đã duyệt)
- Nhân viên đã được admin duyệt
- Xuất hiện trong Customer App
- Có thể nhận việc

### REJECTED (Từ chối)
- Nhân viên bị từ chối
- Không xuất hiện trong Customer App
- Không thể nhận việc

### SUSPENDED (Tạm khóa)
- Nhân viên bị tạm khóa
- Không xuất hiện trong Customer App
- Không thể nhận việc

## Sử Dụng Admin Panel

### Mở Admin Panel
```
http://localhost:3000/admin-approve-workers.html
```

### Tính Năng

#### 1. Thống Kê
- Tổng nhân viên
- Số nhân viên chờ duyệt
- Số nhân viên đã duyệt
- Số nhân viên bị từ chối

#### 2. Danh Sách Nhân Viên
Hiển thị tất cả nhân viên với thông tin:
- Tên, Email, SĐT
- Đánh giá và số reviews
- Số công việc hoàn thành
- Ngày đăng ký
- Trạng thái hiện tại

#### 3. Hành Động
- **Duyệt**: Chấp nhận nhân viên (status → APPROVED)
- **Từ chối**: Từ chối nhân viên (status → REJECTED)

#### 4. Tự Động Refresh
- Tự động cập nhật mỗi 30 giây
- Hoặc click nút 🔄 để refresh thủ công

## API Endpoints

### Lấy Tất Cả Nhân Viên (Admin)
```
GET /api/workers/all
```
Response: Danh sách tất cả nhân viên (bao gồm PENDING, APPROVED, REJECTED)

### Lấy Nhân Viên Đã Duyệt (Customer App)
```
GET /api/workers
```
Response: Chỉ nhân viên có status = APPROVED và is_available = TRUE

### Cập Nhật Trạng Thái Nhân Viên
```
PUT /api/workers/:id/status
Body: { "status": "APPROVED" | "REJECTED" | "SUSPENDED" }
```

## Test Quy Trình

### Bước 1: Đăng Ký Nhân Viên Mới
1. Mở Worker App
2. Đăng ký với:
   - Tên: "Nguyễn Văn Test"
   - Email: "test@example.com"
   - SĐT: "0909999999"
   - Password: "password123"

### Bước 2: Kiểm Tra Admin Panel
1. Mở: http://localhost:3000/admin-approve-workers.html
2. Thấy nhân viên mới với status "Chờ duyệt"
3. Số "Chờ duyệt" tăng lên 1

### Bước 3: Kiểm Tra Customer App
1. Mở Customer App
2. Vào tab "Dịch vụ"
3. Chọn một dịch vụ
4. ❌ Nhân viên mới CHƯA xuất hiện (vì status = PENDING)

### Bước 4: Duyệt Nhân Viên
1. Quay lại Admin Panel
2. Click "✅ Duyệt" cho nhân viên mới
3. Thấy status chuyển sang "Đã duyệt"

### Bước 5: Kiểm Tra Lại Customer App
1. Quay lại Customer App
2. Pull to refresh hoặc restart app
3. Vào tab "Dịch vụ" → Chọn dịch vụ
4. ✅ Nhân viên mới ĐÃ xuất hiện trong danh sách

## Lưu Ý Quan Trọng

### 1. API Server Phải Chạy
```bash
cd appdonnhacuanhanvien
node api-server-example.js
```

### 2. Database Phải Có Dữ Liệu
- Import file: `import-all-data.sql`
- Hoặc đảm bảo database `cleaning_service` đã tồn tại

### 3. Worker Services
Khi duyệt nhân viên, bạn có thể cần gán dịch vụ cho họ:
```sql
-- Gán dịch vụ cho nhân viên
INSERT INTO worker_services (worker_id, service_id) VALUES
('worker_id', 'svc_1'),
('worker_id', 'svc_2');
```

### 4. Customer App Caching
Nếu Customer App không hiển thị nhân viên mới:
- Pull to refresh
- Restart app
- Clear app data

## Troubleshooting

### Admin Panel không load
1. Kiểm tra API server: http://localhost:3000/api/workers/all
2. Kiểm tra console trong browser (F12)
3. Đảm bảo CORS được enable trong API server

### Nhân viên không xuất hiện sau khi duyệt
1. Kiểm tra status trong database:
   ```sql
   SELECT id, name, status FROM workers;
   ```
2. Đảm bảo `is_available = TRUE`
3. Kiểm tra API response: http://localhost:3000/api/workers

### Không thể cập nhật status
1. Kiểm tra API server logs
2. Kiểm tra database connection
3. Thử update trực tiếp trong database:
   ```sql
   UPDATE workers SET status = 'APPROVED' WHERE id = 'worker_id';
   ```

## Tự Động Duyệt (Optional)

Nếu muốn tự động duyệt tất cả nhân viên mới:

```sql
-- Duyệt tất cả nhân viên PENDING
UPDATE workers SET status = 'APPROVED' WHERE status = 'PENDING';
```

Hoặc thêm vào API server:
```javascript
// Auto-approve new workers (for development only)
app.post('/api/workers/register', async (req, res) => {
    // ... existing code ...
    
    // Change status to APPROVED instead of PENDING
    const status = 'APPROVED'; // Auto-approve
    
    // ... rest of code ...
});
```

## Kết Luận

✅ Nhân viên đăng ký → Status = PENDING
✅ Admin duyệt → Status = APPROVED
✅ Nhân viên xuất hiện trong Customer App
✅ Khách hàng có thể đặt dịch vụ với nhân viên đó
