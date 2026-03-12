# Hướng dẫn chuyển Worker App sang MySQL

## ✅ Đã hoàn thành

1. ✅ Xóa Firebase dependencies từ `build.gradle.kts`
2. ✅ Thêm Retrofit dependencies cho MySQL API
3. ✅ Tạo `RetrofitClient.kt` - HTTP client
4. ✅ Tạo `ApiService.kt` - API endpoints
5. ✅ Tạo `MySQLRepository.kt` - Repository mới dùng MySQL
6. ✅ Cập nhật `AppConfig.kt` - Xóa USE_FIREBASE flag
7. ✅ Cập nhật `AuthViewModel.kt` - Dùng MySQLRepository
8. ✅ Cập nhật `JobViewModel.kt` - Dùng MySQLRepository
9. ✅ Xóa `FirebaseRepository.kt`
10. ✅ Xóa `app/google-services.json`
11. ✅ Thêm Internet permissions vào `AndroidManifest.xml`
12. ✅ Thêm `usesCleartextTraffic="true"` cho HTTP support
13. ✅ Đơn giản hóa `ForgotPasswordScreen.kt` - Hiển thị "tính năng đang phát triển"
14. ✅ Đơn giản hóa `PhoneRegisterScreen.kt` - Hiển thị "tính năng đang phát triển"
15. ✅ Fix nullable Worker handling trong `HomeScreen.kt` và `ProfileScreen.kt`
16. ✅ **Build thành công - Không còn lỗi!**

## 🔄 Cần làm tiếp

### Bước 1: Setup MySQL Server

Bạn cần có MySQL server đang chạy với các API endpoints:

**Required Endpoints:**

```
POST /api/workers/register
POST /api/workers/login
GET /api/workers/:id
PUT /api/workers/:id/online
GET /api/jobs/pending
GET /api/jobs/worker/:workerId
PUT /api/jobs/:jobId/accept
PUT /api/jobs/:jobId/status
PUT /api/jobs/:jobId/complete
```

**Cấu trúc Database:**

```sql
CREATE TABLE workers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    completedJobs INT DEFAULT 0,
    averageRating DECIMAL(3,2) DEFAULT 0.0,
    totalEarnings INT DEFAULT 0,
    todayEarnings INT DEFAULT 0,
    isOnline BOOLEAN DEFAULT FALSE,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jobs (
    id VARCHAR(255) PRIMARY KEY,
    serviceId VARCHAR(255),
    serviceName VARCHAR(255),
    customerName VARCHAR(255),
    customerPhone VARCHAR(20),
    customerAddress TEXT,
    workerId VARCHAR(255),
    price INT,
    status ENUM('PENDING', 'ACCEPTED', 'ARRIVING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'),
    scheduledDate BIGINT,
    description TEXT,
    icon VARCHAR(10),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (workerId) REFERENCES workers(id)
);
```

### Bước 2: Cấu hình Server URL (Đã cấu hình sẵn)

File: `app/src/main/java/com/example/donvesinhcuanv/data/api/RetrofitClient.kt`

**Cho Emulator:**
```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/"
```

**Cho Real Device:**
```kotlin
private const val BASE_URL = "http://YOUR_COMPUTER_IP:3000/"
```

Ví dụ: `http://192.168.1.100:3000/`

### Bước 3: Sync & Build (Đã hoàn thành)

1. **Sync Gradle** - Click "Sync Now"
2. **Clean Project** - Build → Clean Project
3. **Rebuild** - Build → Rebuild Project
4. **Run app**

## 📝 Checklist

- [x] Đổi `USE_FIREBASE = false` trong AppConfig
- [x] Update AuthViewModel dùng MySQLRepository
- [x] Update JobViewModel dùng MySQLRepository
- [x] Thêm Internet permissions vào AndroidManifest
- [x] Cấu hình BASE_URL đúng
- [ ] MySQL server đang chạy
- [ ] Database đã được tạo
- [ ] API endpoints hoạt động
- [x] Sync Gradle
- [x] Build thành công
- [ ] Run app và test với MySQL server

## 🚀 Test Flow

1. **Đăng ký:**
   - Mở app
   - Click "Đăng ký"
   - Nhập thông tin
   - Submit → Gọi `POST /api/workers/register`

2. **Đăng nhập:**
   - Nhập email/password
   - Submit → Gọi `POST /api/workers/login`

3. **Xem đơn hàng:**
   - Vào màn hình "Dịch vụ"
   - Load jobs → Gọi `GET /api/jobs/pending`

4. **Nhận đơn:**
   - Click "Nhận đơn"
   - → Gọi `PUT /api/jobs/:jobId/accept`

## ⚠️ Lưu ý

1. **Server phải chạy trước khi test app**
2. **Emulator dùng `10.0.2.2` để truy cập localhost của máy host**
3. **Real device cần dùng IP thật của máy tính**
4. **Firewall có thể chặn kết nối, cần mở port 3000**

## 🔧 Troubleshooting

### Lỗi: "Unable to resolve host"
- Kiểm tra server có đang chạy không
- Kiểm tra BASE_URL đúng chưa
- Ping server từ terminal: `ping 10.0.2.2` (emulator) hoặc `ping YOUR_IP` (device)

### Lỗi: "Connection refused"
- Server chưa chạy
- Port 3000 bị chặn bởi firewall
- Sai BASE_URL

### Lỗi: "Timeout"
- Server quá chậm
- Mạng không ổn định
- Tăng timeout trong RetrofitClient

## 📚 Tài liệu tham khảo

- Retrofit: https://square.github.io/retrofit/
- OkHttp: https://square.github.io/okhttp/
- MySQL: https://dev.mysql.com/doc/

---

## Bước tiếp theo

Sau khi hoàn thành checklist trên, app sẽ chạy hoàn toàn với MySQL, không còn phụ thuộc Firebase nữa!
