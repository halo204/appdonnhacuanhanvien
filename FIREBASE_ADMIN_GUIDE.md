# Hướng dẫn cho Admin Firebase Project

## Để chia sẻ Firebase project với developer khác:

### Bước 1: Thêm Android App (nếu chưa có)

1. Vào Firebase Console: https://console.firebase.google.com/
2. Chọn project của bạn
3. Nhấn biểu tượng Android (</>) để thêm app
4. Điền thông tin:
   - **Android package name**: `com.example.donvesinhcuanv`
   - **App nickname**: CleanService Worker
   - **SHA-1**: Để trống (không bắt buộc)
5. Nhấn "Register app"
6. Tải file `google-services.json`

### Bước 2: Chia sẻ file google-services.json

Gửi file `google-services.json` cho developer qua:
- Email
- Google Drive
- Slack/Teams
- GitHub (private repo)

⚠️ **LƯU Ý**: File này chứa API keys, không nên public

### Bước 3: Thêm developer vào project (Optional)

Để developer có thể xem Firebase Console:

1. Vào **Project Settings** → **Users and permissions**
2. Nhấn "Add member"
3. Nhập email của developer
4. Chọn role:
   - **Viewer**: Chỉ xem
   - **Editor**: Xem và chỉnh sửa
   - **Owner**: Full quyền
5. Nhấn "Add member"

### Bước 4: Cấu hình Authentication

Đảm bảo đã bật Email/Password:

1. Vào **Authentication** → **Sign-in method**
2. Bật **Email/Password**
3. Nhấn "Save"

### Bước 5: Cấu hình Firestore Database

1. Vào **Firestore Database**
2. Nếu chưa có, tạo database mới:
   - Chọn "Start in test mode" (để test)
   - Chọn location gần nhất
3. Tạo collections:
   - `workers` - Lưu thông tin nhân viên
   - `jobs` - Lưu thông tin công việc

### Bước 6: Cấu hình Security Rules

Vào **Firestore Database** → **Rules**, paste code sau:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Workers collection
    match /workers/{workerId} {
      allow read, write: if request.auth != null && request.auth.uid == workerId;
    }
    
    // Jobs collection
    match /jobs/{jobId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null && (
        resource.data.workerId == request.auth.uid ||
        resource.data.status == "PENDING"
      );
      allow delete: if false;
    }
  }
}
```

### Bước 7: Tạo dữ liệu mẫu (Optional)

Để test app, tạo một số job mẫu:

1. Vào **Firestore Database**
2. Tạo collection `jobs`
3. Thêm document với structure:

```json
{
  "serviceId": "S001",
  "serviceName": "Dọn dẹp nhà cửa",
  "customerName": "Nguyễn Văn A",
  "customerPhone": "0901234567",
  "address": "123 Nguyễn Huệ, Q1, TP.HCM",
  "distance": 2.5,
  "scheduledDate": "Timestamp (chọn từ dropdown)",
  "price": 150000,
  "status": "PENDING",
  "description": "Dọn dẹp toàn bộ nhà cửa",
  "icon": "🏠",
  "isNew": true,
  "createdAt": "Timestamp (chọn từ dropdown)"
}
```

## Thông tin cần chia sẻ với developer:

✅ File `google-services.json`
✅ Package name: `com.example.donvesinhcuanv`
✅ Firebase Project ID
✅ Tài khoản test (nếu có)

## Kiểm tra kết nối

Sau khi developer build app:

1. Vào **Authentication** → **Users**
2. Khi developer đăng ký, sẽ thấy user mới xuất hiện
3. Vào **Firestore Database** → **workers**
4. Sẽ thấy document của worker vừa đăng ký

## Troubleshooting

### Developer báo lỗi "API key not valid"
- Kiểm tra file `google-services.json` đã đúng chưa
- Kiểm tra package name trong app khớp với Firebase

### Developer không thấy jobs
- Kiểm tra đã tạo collection `jobs` chưa
- Kiểm tra Security Rules có cho phép đọc không
- Kiểm tra status của job phải là `PENDING`

### Developer không đăng nhập được
- Kiểm tra đã bật Email/Password trong Authentication chưa
- Kiểm tra Security Rules

## Liên hệ

Nếu cần hỗ trợ:
- Firebase Documentation: https://firebase.google.com/docs
- Firebase Support: https://firebase.google.com/support
