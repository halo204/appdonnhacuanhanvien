# Cấu trúc Database chung cho Customer App và Worker App

## Collections trong Firestore

### 1. Collection: `users` (Khách hàng)
Lưu thông tin khách hàng từ Customer App

```json
{
  "userId": "user_uid_from_auth",
  "name": "Nguyễn Văn A",
  "email": "customer@example.com",
  "phone": "0901234567",
  "address": "123 Nguyễn Huệ, Q1, TP.HCM",
  "avatar": "url_to_image",
  "createdAt": "Timestamp",
  "role": "customer"
}
```

### 2. Collection: `workers` (Nhân viên)
Lưu thông tin nhân viên từ Worker App

```json
{
  "workerId": "worker_uid_from_auth",
  "name": "Trần Văn B",
  "email": "worker@example.com",
  "phone": "0909123456",
  "avatar": "url_to_image",
  "isOnline": false,
  "currentLocation": {
    "latitude": 10.762622,
    "longitude": 106.660172
  },
  "completedJobs": 0,
  "averageRating": 4.8,
  "totalEarnings": 1500000,
  "todayEarnings": 0,
  "specialties": ["Dọn dẹp nhà cửa", "Giặt ủi"],
  "createdAt": "Timestamp",
  "role": "worker"
}
```

### 3. Collection: `jobs` (Công việc - QUAN TRỌNG)
Liên kết giữa Customer và Worker

```json
{
  "jobId": "auto_generated_id",
  
  // Thông tin khách hàng
  "customerId": "user_uid",
  "customerName": "Nguyễn Văn A",
  "customerPhone": "0901234567",
  "customerAddress": "123 Nguyễn Huệ, Q1, TP.HCM",
  "customerLocation": {
    "latitude": 10.762622,
    "longitude": 106.660172
  },
  
  // Thông tin dịch vụ
  "serviceId": "S001",
  "serviceName": "Dọn dẹp nhà cửa",
  "serviceIcon": "🏠",
  "description": "Dọn dẹp toàn bộ nhà cửa",
  "price": 150000,
  "scheduledDate": "Timestamp",
  
  // Thông tin nhân viên (null khi chưa có người nhận)
  "workerId": null,
  "workerName": null,
  "workerPhone": null,
  
  // Trạng thái
  "status": "PENDING", // PENDING, ACCEPTED, ARRIVING, IN_PROGRESS, COMPLETED, CANCELLED
  "isNew": true,
  
  // Khoảng cách (tính khi worker nhận)
  "distance": 0,
  
  // Timestamps
  "createdAt": "Timestamp",
  "acceptedAt": null,
  "completedAt": null,
  
  // Đánh giá (sau khi hoàn thành)
  "rating": null,
  "review": null
}
```

### 4. Collection: `services` (Danh sách dịch vụ)
Dùng chung cho cả 2 app

```json
{
  "serviceId": "S001",
  "name": "Dọn dẹp nhà cửa",
  "category": "cleaning",
  "description": "Dọn dẹp toàn bộ nhà cửa, lau chùi, hút bụi",
  "icon": "🏠",
  "basePrice": 150000,
  "duration": 120,
  "isActive": true
}
```

## Luồng hoạt động

### Customer App tạo job:
1. Customer chọn dịch vụ
2. Điền thông tin (địa chỉ, thời gian)
3. Tạo document trong `jobs` collection với:
   - `customerId`: UID của customer
   - `status`: "PENDING"
   - `workerId`: null
   - `isNew`: true

### Worker App nhận job:
1. Worker bật online
2. Lắng nghe jobs với `status = "PENDING"`
3. Worker nhấn "Nhận đơn"
4. Update job:
   - `workerId`: UID của worker
   - `status`: "ACCEPTED"
   - `acceptedAt`: Timestamp
   - `isNew`: false

### Cập nhật trạng thái:
- Worker: "Đang đến" → `status = "ARRIVING"`
- Worker: "Bắt đầu" → `status = "IN_PROGRESS"`
- Worker: "Hoàn thành" → `status = "COMPLETED"`
- Customer có thể theo dõi real-time

## Security Rules

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
    
    // Jobs collection - QUAN TRỌNG
    match /jobs/{jobId} {
      // Cho phép đọc nếu là customer hoặc worker của job đó
      allow read: if request.auth != null && (
        resource.data.customerId == request.auth.uid ||
        resource.data.workerId == request.auth.uid ||
        resource.data.status == "PENDING"
      );
      
      // Customer có thể tạo job mới
      allow create: if request.auth != null && 
        request.resource.data.customerId == request.auth.uid;
      
      // Worker có thể nhận job (update workerId)
      allow update: if request.auth != null && (
        // Worker nhận job PENDING
        (resource.data.status == "PENDING" && 
         request.resource.data.workerId == request.auth.uid) ||
        // Worker cập nhật job của mình
        (resource.data.workerId == request.auth.uid) ||
        // Customer cập nhật job của mình (hủy, đánh giá)
        (resource.data.customerId == request.auth.uid)
      );
      
      // Không cho phép xóa
      allow delete: if false;
    }
    
    // Services collection (read-only cho users)
    match /services/{serviceId} {
      allow read: if true;
      allow write: if false; // Chỉ admin mới được sửa
    }
  }
}
```

## Queries quan trọng

### Customer App:
```kotlin
// Lấy jobs của customer
firestore.collection("jobs")
    .whereEqualTo("customerId", currentUserId)
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .addSnapshotListener { ... }
```

### Worker App:
```kotlin
// Lấy jobs đang chờ (PENDING)
firestore.collection("jobs")
    .whereEqualTo("status", "PENDING")
    .orderBy("createdAt", Query.Direction.ASCENDING)
    .addSnapshotListener { ... }

// Lấy jobs của worker
firestore.collection("jobs")
    .whereEqualTo("workerId", currentWorkerId)
    .whereIn("status", listOf("ACCEPTED", "ARRIVING", "IN_PROGRESS"))
    .addSnapshotListener { ... }
```

## Indexes cần tạo

Trong Firebase Console → Firestore → Indexes:

1. Collection: `jobs`
   - Fields: `status` (Ascending), `createdAt` (Descending)
   
2. Collection: `jobs`
   - Fields: `workerId` (Ascending), `status` (Ascending)
   
3. Collection: `jobs`
   - Fields: `customerId` (Ascending), `createdAt` (Descending)

Firebase sẽ tự động suggest tạo indexes khi bạn chạy queries.
