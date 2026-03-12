# ⚡ Quick Start - Database & API

## 3 bước đơn giản để chạy hệ thống

### Bước 1: Setup Database (2 phút)

```bash
# Login MySQL
mysql -u root -p

# Import database
source database-schema.sql
source sample-data.sql
```

### Bước 2: Chạy API Server (1 phút)

```bash
# Cài đặt
npm install

# Cấu hình
cp .env.example .env
# Sửa DB_PASSWORD trong .env

# Chạy
npm start
```

Server chạy tại: `http://localhost:3000`

### Bước 3: Cập nhật Apps

**Worker App:**
```kotlin
// File: RetrofitClient.kt
private const val BASE_URL = "http://10.0.2.2:3000/"  // Emulator
// private const val BASE_URL = "http://192.168.1.100:3000/"  // Real device
```

**Customer App:**
Tương tự, cập nhật BASE_URL trong RetrofitClient.

## ✅ Test

```bash
# Test API
curl http://localhost:3000/api/services

# Test login
curl -X POST http://localhost:3000/api/workers/login \
  -H "Content-Type: application/json" \
  -d '{"email":"worker1@example.com","password":"password123"}'
```

## 📱 Chạy Apps

1. Build Worker App: `./gradlew assembleDebug`
2. Build Customer App: `./gradlew assembleDebug`
3. Install lên device/emulator
4. Test đăng nhập, tạo đơn, nhận đơn

## 🎯 Sample Accounts

**Worker:**
- Email: `worker1@example.com`
- Password: `password123`

**Customer:**
- Email: `customer1@example.com`
- Password: `password123`

---

**Xong!** Hệ thống đã sẵn sàng. Chi tiết xem [README_DATABASE.md](./README_DATABASE.md)
