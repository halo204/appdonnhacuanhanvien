# Hướng dẫn Setup Database và API Server

## 📋 Tổng quan

Database MySQL này liên kết cả 2 app:
- **Worker App** (appdonnhacuanhanvien) - App cho nhân viên
- **Customer App** (Cleaner) - App cho khách hàng

## 🗄️ Cấu trúc Database

### Tables chính:

1. **users** - Khách hàng
2. **workers** - Nhân viên  
3. **services** - Dịch vụ
4. **bookings** - Đơn hàng (liên kết users và workers)
5. **reviews** - Đánh giá
6. **addresses** - Địa chỉ khách hàng
7. **booking_updates** - Cập nhật trạng thái real-time
8. **worker_locations** - Vị trí nhân viên real-time
9. **earnings_history** - Lịch sử thu nhập
10. **notifications** - Thông báo
11. **service_categories** - Danh mục dịch vụ

## 🚀 Bước 1: Cài đặt MySQL

### Windows:
```bash
# Download MySQL từ: https://dev.mysql.com/downloads/installer/
# Hoặc dùng XAMPP: https://www.apachefriends.org/
```

### Mac:
```bash
brew install mysql
brew services start mysql
```

### Linux:
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
```

## 🔧 Bước 2: Tạo Database

```bash
# Login vào MySQL
mysql -u root -p

# Chạy script tạo database
source database-schema.sql

# Chạy script insert sample data
source sample-data.sql
```

Hoặc import trực tiếp:
```bash
mysql -u root -p < database-schema.sql
mysql -u root -p < sample-data.sql
```

## 📊 Bước 3: Kiểm tra Database

```sql
USE cleaning_service;

-- Xem tất cả tables
SHOW TABLES;

-- Kiểm tra dữ liệu
SELECT * FROM users;
SELECT * FROM workers;
SELECT * FROM services;
SELECT * FROM bookings;
```

## 🌐 Bước 4: Tạo API Server

Bạn cần tạo REST API server với các endpoints sau:

### API Endpoints cho Worker App:

```
POST   /api/workers/register          - Đăng ký nhân viên
POST   /api/workers/login             - Đăng nhập
GET    /api/workers/:id               - Lấy thông tin nhân viên
PUT    /api/workers/:id/online        - Cập nhật trạng thái online
GET    /api/jobs/pending              - Lấy danh sách đơn chờ
GET    /api/jobs/worker/:workerId     - Lấy đơn của nhân viên
PUT    /api/jobs/:jobId/accept        - Nhận đơn
PUT    /api/jobs/:jobId/status        - Cập nhật trạng thái đơn
PUT    /api/jobs/:jobId/complete      - Hoàn thành đơn
```

### API Endpoints cho Customer App:

```
POST   /api/users/register            - Đăng ký khách hàng
POST   /api/users/login               - Đăng nhập
GET    /api/users/:id                 - Lấy thông tin khách hàng
GET    /api/services                  - Lấy danh sách dịch vụ
GET    /api/services/:id              - Chi tiết dịch vụ
GET    /api/workers                   - Lấy danh sách nhân viên
GET    /api/workers/:id               - Chi tiết nhân viên
POST   /api/bookings                  - Tạo đơn hàng mới
GET    /api/bookings/user/:userId     - Lấy đơn của khách hàng
GET    /api/bookings/:id              - Chi tiết đơn hàng
PUT    /api/bookings/:id/cancel       - Hủy đơn
POST   /api/reviews                   - Tạo đánh giá
GET    /api/reviews/worker/:workerId  - Đánh giá của nhân viên
GET    /api/addresses/user/:userId    - Địa chỉ của khách hàng
POST   /api/addresses                 - Thêm địa chỉ mới
```

## 💻 Bước 5: Tạo Node.js API Server (Ví dụ)

### 5.1. Cài đặt dependencies:

```bash
mkdir cleaning-service-api
cd cleaning-service-api
npm init -y
npm install express mysql2 bcrypt jsonwebtoken cors body-parser dotenv
```

### 5.2. Tạo file `.env`:

```env
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=cleaning_service
PORT=3000
JWT_SECRET=your_secret_key_here
```

### 5.3. Tạo file `server.js`:

Xem file `api-server-example.js` để có code mẫu hoàn chỉnh.

## 🔐 Bước 6: Security

### Hash passwords:
```javascript
const bcrypt = require('bcrypt');
const hashedPassword = await bcrypt.hash(password, 10);
```

### JWT Authentication:
```javascript
const jwt = require('jsonwebtoken');
const token = jwt.sign({ userId: user.id }, process.env.JWT_SECRET);
```

## 🧪 Bước 7: Test API

### Test với curl:

```bash
# Register worker
curl -X POST http://localhost:3000/api/workers/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Worker",
    "email": "test@worker.com",
    "password": "password123",
    "phone": "0901234567"
  }'

# Login worker
curl -X POST http://localhost:3000/api/workers/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@worker.com",
    "password": "password123"
  }'

# Get pending jobs
curl http://localhost:3000/api/jobs/pending
```

### Test với Postman:
1. Import collection từ file `api-collection.json`
2. Test từng endpoint
3. Kiểm tra response

## 📱 Bước 8: Kết nối với Apps

### Worker App:
File: `appdonnhacuanhanvien/app/src/main/java/com/example/donvesinhcuanv/data/api/RetrofitClient.kt`

```kotlin
// Cho Emulator
private const val BASE_URL = "http://10.0.2.2:3000/"

// Cho Real Device
private const val BASE_URL = "http://YOUR_COMPUTER_IP:3000/"
```

### Customer App:
Tương tự, cập nhật BASE_URL trong RetrofitClient của Customer App.

## 🔄 Bước 9: Sync Data giữa 2 Apps

### Booking Flow:
1. **Customer App** tạo booking → INSERT vào table `bookings`
2. **Worker App** nhận thông báo → GET `/api/jobs/pending`
3. **Worker App** nhận đơn → PUT `/api/jobs/:id/accept`
4. **Customer App** nhận update → GET `/api/bookings/:id`

### Real-time Updates (Optional):
- Dùng WebSocket hoặc Socket.io
- Hoặc polling mỗi 5-10 giây

## 📊 Bước 10: Monitoring

### Check database size:
```sql
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS "Size (MB)"
FROM information_schema.TABLES
WHERE table_schema = "cleaning_service"
ORDER BY (data_length + index_length) DESC;
```

### Check active connections:
```sql
SHOW PROCESSLIST;
```

## 🐛 Troubleshooting

### Lỗi: "Access denied for user"
```bash
# Reset MySQL password
mysql -u root
ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
```

### Lỗi: "Can't connect to MySQL server"
```bash
# Check MySQL service
sudo systemctl status mysql  # Linux
brew services list           # Mac
```

### Lỗi: "Table doesn't exist"
```bash
# Re-run schema
mysql -u root -p cleaning_service < database-schema.sql
```

## 📚 Tài liệu tham khảo

- MySQL: https://dev.mysql.com/doc/
- Express.js: https://expressjs.com/
- JWT: https://jwt.io/
- Bcrypt: https://www.npmjs.com/package/bcrypt

---

**Hoàn thành!** Database đã sẵn sàng cho cả 2 apps.
