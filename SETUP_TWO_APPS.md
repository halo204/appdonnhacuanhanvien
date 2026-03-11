# Setup hệ thống 2 App (Customer + Worker)

## Tổng quan

Hệ thống gồm 2 app chia sẻ chung 1 Firebase project:

1. **Customer App** - Khách hàng đặt dịch vụ
2. **Worker App** - Nhân viên nhận và xử lý đơn (app này)

## Bước 1: Tạo Firebase Project

1. Vào https://console.firebase.google.com/
2. Tạo project mới: `CleanService`
3. Bật Google Analytics (optional)

## Bước 2: Thêm 2 Android Apps vào cùng 1 project

### App 1: Customer App
1. Nhấn biểu tượng Android
2. Package name: `com.example.cleanservice.customer`
3. App nickname: `CleanService Customer`
4. Tải `google-services.json` → Đặt vào Customer App

### App 2: Worker App (app này)
1. Nhấn "Add app" → Chọn Android
2. Package name: `com.example.cleanservice.worker`
3. App nickname: `CleanService Worker`
4. Tải `google-services.json` → Đặt vào Worker App

⚠️ **LƯU Ý**: Mỗi app có file `google-services.json` riêng!

## Bước 3: Cấu hình Authentication

1. Vào **Authentication** → **Sign-in method**
2. Bật **Email/Password**
3. Nhấn "Save"

## Bước 4: Tạo Firestore Database

1. Vào **Firestore Database**
2. Nhấn "Create database"
3. Chọn "Start in test mode"
4. Location: `asia-southeast1`
5. Nhấn "Enable"

## Bước 5: Tạo Collections

### Collection: `users`
Cho Customer App

Click "Start collection" → Collection ID: `users`

### Collection: `workers`
Cho Worker App

Click "Start collection" → Collection ID: `workers`

### Collection: `jobs`
Chia sẻ giữa 2 app (QUAN TRỌNG)

Click "Start collection" → Collection ID: `jobs`

### Collection: `services`
Danh sách dịch vụ (read-only)

Tạo các document mẫu:

**Document ID: S001**
```json
{
  "serviceId": "S001",
  "name": "Dọn dẹp nhà cửa",
  "category": "cleaning",
  "description": "Dọn dẹp toàn bộ nhà cửa, lau chùi, hút bụi, sắp xếp đồ đạc gọn gàng",
  "icon": "🏠",
  "basePrice": 150000,
  "duration": 120,
  "isActive": true
}
```

**Document ID: S002**
```json
{
  "serviceId": "S002",
  "name": "Giặt ủi quần áo",
  "category": "laundry",
  "description": "Giặt và ủi quần áo chuyên nghiệp, sử dụng hóa chất an toàn",
  "icon": "👔",
  "basePrice": 100000,
  "duration": 90,
  "isActive": true
}
```

**Document ID: S003**
```json
{
  "serviceId": "S003",
  "name": "Vệ sinh máy lạnh",
  "category": "maintenance",
  "description": "Vệ sinh và bảo dưỡng máy lạnh, kiểm tra gas, làm sạch dàn nóng",
  "icon": "❄️",
  "basePrice": 200000,
  "duration": 60,
  "isActive": true
}
```

## Bước 6: Cấu hình Security Rules

Vào **Firestore Database** → **Rules**, paste code:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users collection (Customers)
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Workers collection
    match /workers/{workerId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == workerId;
    }
    
    // Jobs collection - Chia sẻ giữa 2 app
    match /jobs/{jobId} {
      allow read: if request.auth != null && (
        resource.data.customerId == request.auth.uid ||
        resource.data.workerId == request.auth.uid ||
        resource.data.status == "PENDING"
      );
      
      allow create: if request.auth != null && 
        request.resource.data.customerId == request.auth.uid;
      
      allow update: if request.auth != null && (
        (resource.data.status == "PENDING" && 
         request.resource.data.workerId == request.auth.uid) ||
        (resource.data.workerId == request.auth.uid) ||
        (resource.data.customerId == request.auth.uid)
      );
      
      allow delete: if false;
    }
    
    // Services collection
    match /services/{serviceId} {
      allow read: if true;
      allow write: if false;
    }
  }
}
```

Nhấn "Publish"

## Bước 7: Cập nhật Package Name

### Worker App (app này)

File: `app/build.gradle.kts`
```kotlin
android {
    namespace = "com.example.cleanservice.worker"
    
    defaultConfig {
        applicationId = "com.example.cleanservice.worker"
        // ...
    }
}
```

### Customer App

File: `app/build.gradle.kts`
```kotlin
android {
    namespace = "com.example.cleanservice.customer"
    
    defaultConfig {
        applicationId = "com.example.cleanservice.customer"
        // ...
    }
}
```

## Bước 8: Test hệ thống

### Test Customer App:
1. Đăng ký tài khoản customer
2. Chọn dịch vụ
3. Tạo đơn hàng
4. Kiểm tra Firestore → `jobs` collection → Sẽ thấy job mới với `status = "PENDING"`

### Test Worker App:
1. Đăng ký tài khoản worker
2. Bật online
3. Sẽ thấy job từ customer
4. Nhận đơn
5. Kiểm tra Firestore → Job sẽ có `workerId` và `status = "ACCEPTED"`

### Test Real-time:
1. Mở cả 2 app
2. Customer tạo đơn → Worker thấy ngay lập tức
3. Worker nhận đơn → Customer thấy trạng thái cập nhật
4. Worker cập nhật trạng thái → Customer theo dõi real-time

## Luồng dữ liệu

```
Customer App                    Firestore                    Worker App
     |                             |                             |
     |-- Tạo job ----------------->|                             |
     |   (status: PENDING)         |                             |
     |                             |<---- Lắng nghe PENDING -----|
     |                             |                             |
     |                             |<---- Nhận đơn --------------|
     |                             |   (update workerId)         |
     |<--- Lắng nghe job của mình -|                             |
     |   (thấy worker đã nhận)     |                             |
     |                             |                             |
     |<--- Real-time updates ------|<--- Cập nhật trạng thái ----|
     |   (ARRIVING, IN_PROGRESS,   |                             |
     |    COMPLETED)               |                             |
```

## Troubleshooting

### Customer tạo job nhưng Worker không thấy
- Kiểm tra Security Rules
- Kiểm tra `status` của job phải là `"PENDING"`
- Kiểm tra Worker đã bật online chưa

### Worker nhận job nhưng Customer không thấy
- Kiểm tra Customer có lắng nghe job của mình không
- Kiểm tra `customerId` trong job khớp với UID của customer

### Lỗi permission denied
- Kiểm tra Security Rules
- Kiểm tra user đã đăng nhập chưa
- Kiểm tra UID khớp với document ID

## Tài liệu tham khảo

- `SHARED_DATABASE_STRUCTURE.md` - Cấu trúc database chi tiết
- `FIREBASE_SETUP.md` - Setup Firebase cơ bản
- Firebase Documentation: https://firebase.google.com/docs
