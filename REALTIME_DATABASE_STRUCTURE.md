# Cấu trúc Realtime Database cho Customer App và Worker App

## Cấu trúc JSON

```json
{
  "users": {
    "user_uid_1": {
      "userId": "user_uid_1",
      "name": "Nguyễn Văn A",
      "email": "customer@example.com",
      "phone": "0901234567",
      "address": "123 Nguyễn Huệ, Q1, TP.HCM",
      "avatar": "url_to_image",
      "createdAt": 1234567890000,
      "role": "customer"
    }
  },
  
  "workers": {
    "worker_uid_1": {
      "id": "worker_uid_1",
      "name": "Trần Văn B",
      "email": "worker@example.com",
      "phone": "0909123456",
      "avatar": "url_to_image",
      "isOnline": false,
      "completedJobs": 0,
      "averageRating": 4.8,
      "totalEarnings": 1500000,
      "todayEarnings": 0,
      "specialties": ["Dọn dẹp nhà cửa", "Giặt ủi"],
      "createdAt": 1234567890000,
      "role": "worker"
    }
  },
  
  "jobs": {
    "job_id_1": {
      "jobId": "job_id_1",
      "customerId": "user_uid_1",
      "customerName": "Nguyễn Văn A",
      "customerPhone": "0901234567",
      "customerAddress": "123 Nguyễn Huệ, Q1, TP.HCM",
      "serviceId": "S001",
      "serviceName": "Dọn dẹp nhà cửa",
      "serviceIcon": "🏠",
      "description": "Dọn dẹp toàn bộ nhà cửa",
      "price": 150000,
      "scheduledDate": 1234567890000,
      "workerId": null,
      "workerName": null,
      "workerPhone": null,
      "status": "PENDING",
      "isNew": true,
      "distance": 0,
      "createdAt": 1234567890000,
      "acceptedAt": null,
      "completedAt": null,
      "rating": null,
      "review": null
    }
  },
  
  "services": {
    "S001": {
      "serviceId": "S001",
      "name": "Dọn dẹp nhà cửa",
      "category": "cleaning",
      "description": "Dọn dẹp toàn bộ nhà cửa, lau chùi, hút bụi",
      "icon": "🏠",
      "basePrice": 150000,
      "duration": 120,
      "isActive": true
    },
    "S002": {
      "serviceId": "S002",
      "name": "Giặt ủi quần áo",
      "category": "laundry",
      "description": "Giặt và ủi quần áo chuyên nghiệp",
      "icon": "👔",
      "basePrice": 100000,
      "duration": 90,
      "isActive": true
    },
    "S003": {
      "serviceId": "S003",
      "name": "Vệ sinh máy lạnh",
      "category": "maintenance",
      "description": "Vệ sinh và bảo dưỡng máy lạnh",
      "icon": "❄️",
      "basePrice": 200000,
      "duration": 60,
      "isActive": true
    }
  }
}
```

## Luồng hoạt động

### Customer App tạo job:
1. Customer chọn dịch vụ
2. Điền thông tin (địa chỉ, thời gian)
3. Push vào `/jobs` với:
   - `customerId`: UID của customer
   - `status`: "PENDING"
   - `workerId`: null
   - `isNew`: true

### Worker App nhận job:
1. Worker bật online
2. Lắng nghe `/jobs` với query `orderByChild("status").equalTo("PENDING")`
3. Worker nhấn "Nhận đơn"
4. Update job:
   - `workerId`: UID của worker
   - `status`: "ACCEPTED"
   - `acceptedAt`: ServerValue.TIMESTAMP
   - `isNew`: false

### Cập nhật trạng thái:
- Worker: "Đang đến" → `status = "ARRIVING"`
- Worker: "Bắt đầu" → `status = "IN_PROGRESS"`
- Worker: "Hoàn thành" → `status = "COMPLETED"`
- Customer có thể theo dõi real-time

## Security Rules

```json
{
  "rules": {
    "users": {
      "$userId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $userId"
      }
    },
    
    "workers": {
      "$workerId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $workerId"
      }
    },
    
    "jobs": {
      "$jobId": {
        ".read": "auth != null && (
          data.child('customerId').val() == auth.uid ||
          data.child('workerId').val() == auth.uid ||
          data.child('status').val() == 'PENDING'
        )",
        
        ".write": "auth != null && (
          (!data.exists() && newData.child('customerId').val() == auth.uid) ||
          (data.child('status').val() == 'PENDING' && newData.child('workerId').val() == auth.uid) ||
          (data.child('workerId').val() == auth.uid) ||
          (data.child('customerId').val() == auth.uid)
        )"
      }
    },
    
    "services": {
      ".read": true,
      ".write": false
    }
  }
}
```

## Queries quan trọng

### Customer App:
```kotlin
// Lấy jobs của customer
database.getReference("jobs")
    .orderByChild("customerId")
    .equalTo(currentUserId)
    .addValueEventListener { ... }
```

### Worker App:
```kotlin
// Lấy jobs đang chờ (PENDING)
database.getReference("jobs")
    .orderByChild("status")
    .equalTo("PENDING")
    .addValueEventListener { ... }

// Lấy jobs của worker
database.getReference("jobs")
    .addValueEventListener { snapshot ->
        // Filter by workerId and status in code
    }
```

## Indexes

Trong Firebase Console → Realtime Database → Rules, thêm indexes:

```json
{
  "rules": {
    "jobs": {
      ".indexOn": ["status", "customerId", "workerId", "createdAt"]
    }
  }
}
```

## Ưu điểm Realtime Database so với Firestore

1. **Nhanh hơn**: Latency thấp hơn, phù hợp cho real-time
2. **Đơn giản hơn**: Cấu trúc JSON dễ hiểu
3. **Rẻ hơn**: Chi phí thấp hơn cho read/write operations
4. **Offline support**: Tự động sync khi online lại
5. **Real-time**: Cập nhật tức thì không cần polling

## Setup trong Firebase Console

1. Vào **Realtime Database** → **Create Database**
2. Chọn location: **asia-southeast1**
3. Start in **test mode** (để test dễ dàng)
4. Sau đó cập nhật Security Rules như trên
5. Thêm dữ liệu mẫu vào `services`
