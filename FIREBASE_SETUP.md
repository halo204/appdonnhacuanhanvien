# Hướng dẫn cấu hình Firebase cho ứng dụng CleanService Worker

## Bước 1: Tạo Firebase Project

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Nhấn "Add project" hoặc "Thêm dự án"
3. Đặt tên project: `donvesinhcuanv` (hoặc tên bạn muốn)
4. Tắt Google Analytics (không bắt buộc)
5. Nhấn "Create project"

## Bước 2: Thêm Android App

1. Trong Firebase Console, chọn project vừa tạo
2. Nhấn biểu tượng Android để thêm app
3. Điền thông tin:
   - **Android package name**: `com.example.donvesinhcuanv`
   - **App nickname**: CleanService Worker (tùy chọn)
   - **Debug signing certificate SHA-1**: Để trống (không bắt buộc)
4. Nhấn "Register app"

## Bước 3: Tải google-services.json

1. Sau khi đăng ký app, Firebase sẽ cho bạn tải file `google-services.json`
2. Tải file này về
3. **QUAN TRỌNG**: Copy file `google-services.json` vào thư mục `app/` của project
   ```
   donvesinhcuanv/
   └── app/
       └── google-services.json  <-- Đặt file ở đây
   ```
4. Thay thế file `google-services.json` mẫu hiện tại

## Bước 4: Cấu hình Authentication

1. Trong Firebase Console, vào **Authentication**
2. Nhấn "Get started"
3. Chọn tab "Sign-in method"
4. Bật **Email/Password**:
   - Nhấn vào "Email/Password"
   - Bật toggle "Enable"
   - Nhấn "Save"

## Bước 5: Cấu hình Firestore Database

1. Trong Firebase Console, vào **Firestore Database**
2. Nhấn "Create database"
3. Chọn "Start in test mode" (để test, sau này sẽ đổi sang production mode)
4. Chọn location gần nhất (ví dụ: asia-southeast1)
5. Nhấn "Enable"

## Bước 6: Tạo Collections trong Firestore

### Collection: workers
Lưu thông tin nhân viên

Cấu trúc document:
```json
{
  "id": "user_uid",
  "name": "Nguyễn Văn A",
  "email": "user@example.com",
  "phone": "0909123456",
  "completedJobs": 0,
  "averageRating": 0.0,
  "totalEarnings": 0,
  "todayEarnings": 0,
  "isOnline": false,
  "specialties": ["Dọn dẹp nhà cửa", "Giặt ủi"],
  "createdAt": 1234567890
}
```

### Collection: jobs
Lưu thông tin công việc

Cấu trúc document:
```json
{
  "serviceId": "S001",
  "serviceName": "Dọn dẹp nhà cửa",
  "customerName": "Trần Văn B",
  "customerPhone": "0901234567",
  "address": "123 Nguyễn Huệ, Q1, TP.HCM",
  "distance": 2.5,
  "scheduledDate": "Timestamp",
  "price": 150000,
  "status": "PENDING",
  "description": "Dọn dẹp toàn bộ nhà",
  "icon": "🏠",
  "isNew": true,
  "workerId": "user_uid",
  "createdAt": 1234567890
}
```

## Bước 7: Cấu hình Security Rules

1. Trong Firestore Database, chọn tab "Rules"
2. Thay thế rules bằng code sau:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Workers collection
    match /workers/{workerId} {
      // Cho phép đọc và ghi nếu user đã đăng nhập và là chính họ
      allow read, write: if request.auth != null && request.auth.uid == workerId;
    }
    
    // Jobs collection
    match /jobs/{jobId} {
      // Cho phép đọc tất cả jobs nếu đã đăng nhập
      allow read: if request.auth != null;
      
      // Cho phép tạo job mới (từ app khách hàng)
      allow create: if request.auth != null;
      
      // Cho phép cập nhật job nếu là worker được assign hoặc job đang pending
      allow update: if request.auth != null && (
        resource.data.workerId == request.auth.uid ||
        resource.data.status == "PENDING"
      );
      
      // Không cho phép xóa
      allow delete: if false;
    }
  }
}
```

3. Nhấn "Publish"

## Bước 8: Tạo dữ liệu mẫu (Optional)

Để test app, bạn có thể tạo một số job mẫu trong Firestore:

1. Vào Firestore Database
2. Nhấn "Start collection"
3. Collection ID: `jobs`
4. Thêm document với các field như cấu trúc ở trên
5. Đặt `status` = `PENDING` để job hiển thị trong app

## Bước 9: Build và chạy app

```bash
./gradlew assembleDebug
```

## Kiểm tra kết nối

1. Chạy app trên thiết bị/emulator
2. Đăng ký tài khoản mới
3. Kiểm tra trong Firebase Console:
   - **Authentication** → Users: Sẽ thấy user vừa đăng ký
   - **Firestore** → workers: Sẽ thấy document của worker

## Lưu ý quan trọng

- ⚠️ File `google-services.json` chứa thông tin nhạy cảm, không commit lên Git
- ⚠️ Test mode của Firestore cho phép mọi người đọc/ghi, chỉ dùng để test
- ⚠️ Sau khi test xong, đổi sang production mode với rules bảo mật phù hợp
- ⚠️ Nếu thay đổi package name, phải tạo lại app trong Firebase Console

## Troubleshooting

### Lỗi: "No matching client found"
- Kiểm tra package name trong `google-services.json` phải là `com.example.donvesinhcuanv`
- Kiểm tra file `google-services.json` đã đặt đúng trong thư mục `app/`

### Lỗi: "FirebaseApp initialization unsuccessful"
- Kiểm tra file `google-services.json` có đúng format không
- Clean và rebuild project: `./gradlew clean assembleDebug`

### Không thấy jobs trong app
- Kiểm tra đã tạo collection `jobs` trong Firestore chưa
- Kiểm tra status của job phải là `PENDING`
- Kiểm tra Security Rules có cho phép đọc không

## Liên hệ hỗ trợ

Nếu gặp vấn đề, tham khảo:
- [Firebase Documentation](https://firebase.google.com/docs)
- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
