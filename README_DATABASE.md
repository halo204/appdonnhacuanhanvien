# 🗄️ Database & API Setup - Cleaning Service

## 📋 Tổng quan

Hệ thống database MySQL liên kết 2 ứng dụng:
- **Worker App** (appdonnhacuanhanvien) - Ứng dụng cho nhân viên
- **Customer App** (Cleaner) - Ứng dụng cho khách hàng

## 📁 Files trong package

```
├── database-schema.sql          # Schema database đầy đủ
├── sample-data.sql              # Dữ liệu mẫu
├── api-server-example.js        # API server Node.js
├── package.json                 # Dependencies Node.js
├── .env.example                 # Cấu hình môi trường
├── DATABASE_SETUP_GUIDE.md      # Hướng dẫn chi tiết
└── README_DATABASE.md           # File này
```

## 🚀 Quick Start

### 1. Cài đặt MySQL

```bash
# Windows: Download từ https://dev.mysql.com/downloads/installer/
# Mac: brew install mysql
# Linux: sudo apt install mysql-server
```

### 2. Tạo Database

```bash
# Login MySQL
mysql -u root -p

# Import schema
source database-schema.sql

# Import sample data
source sample-data.sql
```

### 3. Setup API Server

```bash
# Cài đặt Node.js dependencies
npm install

# Copy và cấu hình .env
cp .env.example .env
# Sửa DB_PASSWORD trong file .env

# Chạy server
npm start
```

Server sẽ chạy tại: `http://localhost:3000`

### 4. Test API

```bash
# Test worker login
curl -X POST http://localhost:3000/api/workers/login \
  -H "Content-Type: application/json" \
  -d '{"email":"worker1@example.com","password":"password123"}'

# Test get services
curl http://localhost:3000/api/services
```

## 📊 Database Schema

### Tables chính:

| Table | Mô tả | Số cột |
|-------|-------|--------|
| users | Khách hàng | 8 |
| workers | Nhân viên | 23 |
| services | Dịch vụ | 10 |
| bookings | Đơn hàng | 25 |
| reviews | Đánh giá | 15 |
| addresses | Địa chỉ | 8 |
| booking_updates | Cập nhật real-time | 9 |
| worker_locations | Vị trí nhân viên | 7 |
| earnings_history | Lịch sử thu nhập | 6 |
| notifications | Thông báo | 9 |
| service_categories | Danh mục dịch vụ | 6 |

## 🔗 API Endpoints

### Worker App Endpoints:

```
POST   /api/workers/register          # Đăng ký nhân viên
POST   /api/workers/login             # Đăng nhập
GET    /api/workers/:id               # Thông tin nhân viên
PUT    /api/workers/:id/online        # Cập nhật online status
GET    /api/jobs/pending              # Danh sách đơn chờ
GET    /api/jobs/worker/:workerId     # Đơn của nhân viên
PUT    /api/jobs/:jobId/accept        # Nhận đơn
PUT    /api/jobs/:jobId/status        # Cập nhật trạng thái
PUT    /api/jobs/:jobId/complete      # Hoàn thành đơn
```

### Customer App Endpoints:

```
POST   /api/users/register            # Đăng ký khách hàng
POST   /api/users/login               # Đăng nhập
GET    /api/services                  # Danh sách dịch vụ
GET    /api/workers                   # Danh sách nhân viên
POST   /api/bookings                  # Tạo đơn hàng
GET    /api/bookings/user/:userId     # Đơn của khách hàng
```

## 🔄 Data Flow

### Booking Flow:

```
1. Customer App → POST /api/bookings
   ↓
2. Database → INSERT bookings (status: PENDING)
   ↓
3. Worker App → GET /api/jobs/pending
   ↓
4. Worker nhận đơn → PUT /api/jobs/:id/accept
   ↓
5. Database → UPDATE bookings (status: WORKER_ASSIGNED)
   ↓
6. Customer App → GET /api/bookings/user/:userId (nhận update)
```

## 🔐 Security

### Password Hashing:
```javascript
const bcrypt = require('bcrypt');
const hashedPassword = await bcrypt.hash(password, 10);
```

### JWT Authentication:
```javascript
const jwt = require('jsonwebtoken');
const token = jwt.sign({ userId }, process.env.JWT_SECRET);
```

## 📱 Kết nối với Apps

### Worker App:
File: `appdonnhacuanhanvien/app/src/main/java/com/example/donvesinhcuanv/data/api/RetrofitClient.kt`

```kotlin
// Emulator
private const val BASE_URL = "http://10.0.2.2:3000/"

// Real Device  
private const val BASE_URL = "http://192.168.1.100:3000/"
```

### Customer App:
Tương tự, cập nhật BASE_URL trong RetrofitClient.

## 🧪 Testing

### Sample Accounts:

**Workers:**
- Email: `worker1@example.com` | Password: `password123`
- Email: `worker2@example.com` | Password: `password123`

**Customers:**
- Email: `customer1@example.com` | Password: `password123`
- Email: `customer2@example.com` | Password: `password123`

### Test với Postman:

1. Import collection
2. Set base URL: `http://localhost:3000`
3. Test endpoints

## 📊 Monitoring

### Check database:
```sql
USE cleaning_service;
SHOW TABLES;
SELECT COUNT(*) FROM bookings;
SELECT COUNT(*) FROM workers WHERE is_online = TRUE;
```

### Check API logs:
```bash
# Server logs sẽ hiển thị mọi request
npm start
```

## 🐛 Troubleshooting

### Lỗi: "Cannot connect to MySQL"
```bash
# Check MySQL service
sudo systemctl status mysql  # Linux
brew services list           # Mac

# Restart MySQL
sudo systemctl restart mysql
```

### Lỗi: "Access denied"
```bash
# Reset password
mysql -u root
ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
```

### Lỗi: "Port 3000 already in use"
```bash
# Đổi PORT trong .env
PORT=3001

# Hoặc kill process
lsof -ti:3000 | xargs kill
```

## 📚 Tài liệu

- [DATABASE_SETUP_GUIDE.md](./DATABASE_SETUP_GUIDE.md) - Hướng dẫn chi tiết
- [database-schema.sql](./database-schema.sql) - Schema đầy đủ
- [api-server-example.js](./api-server-example.js) - Code API server

## ✅ Checklist

- [ ] MySQL đã cài đặt
- [ ] Database đã tạo (cleaning_service)
- [ ] Tables đã import
- [ ] Sample data đã insert
- [ ] Node.js đã cài đặt
- [ ] Dependencies đã install (npm install)
- [ ] File .env đã cấu hình
- [ ] API server đang chạy
- [ ] Test API thành công
- [ ] Worker App đã cập nhật BASE_URL
- [ ] Customer App đã cập nhật BASE_URL

## 🎉 Hoàn thành!

Database và API server đã sẵn sàng cho cả 2 apps!

---

**Liên hệ:** Nếu có vấn đề, check file DATABASE_SETUP_GUIDE.md
