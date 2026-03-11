# Kết nối đến Firebase Project có sẵn

## Các bước thực hiện:

### 1. Xin file google-services.json

Liên hệ người quản lý Firebase project và xin file `google-services.json`

Họ cần:
- Vào Firebase Console
- Project Settings → General
- Your apps → Android app
- Download google-services.json

### 2. Đặt file vào project

Copy file `google-services.json` vào:
```
donvesinhcuanv/
└── app/
    └── google-services.json  <-- Đặt ở đây (thay thế file cũ)
```

### 3. Kiểm tra package name

Hỏi admin Firebase: Package name của app là gì?

Nếu khác `com.example.donvesinhcuanv`, bạn cần đổi trong:

**File: app/build.gradle.kts**
```kotlin
android {
    namespace = "PACKAGE_NAME_MỚI"
    
    defaultConfig {
        applicationId = "PACKAGE_NAME_MỚI"
        // ...
    }
}
```

### 4. Bật Firebase mode

File đã được cập nhật tự động: `USE_FIREBASE = true`

### 5. Build app

```bash
./gradlew clean assembleDebug
```

### 6. Test kết nối

1. Cài app lên điện thoại
2. Đăng ký tài khoản mới
3. Kiểm tra với admin:
   - Firebase Console → Authentication → Users
   - Sẽ thấy user vừa đăng ký

## Thông tin cần hỏi admin:

- [ ] File `google-services.json`
- [ ] Package name của app
- [ ] Firebase Project ID (optional)
- [ ] Tài khoản test (nếu có)

## Nếu gặp lỗi:

### "No matching client found"
→ Package name không khớp với Firebase
→ Hỏi lại admin package name đúng

### "API key not valid"
→ File `google-services.json` sai hoặc cũ
→ Xin lại file mới từ admin

### Không đăng nhập được
→ Admin chưa bật Email/Password trong Authentication
→ Nhờ admin bật theo hướng dẫn trong `FIREBASE_ADMIN_GUIDE.md`

### Không thấy jobs
→ Admin chưa tạo collection `jobs` hoặc chưa có dữ liệu
→ Nhờ admin tạo job mẫu

## Gửi cho admin

Nếu admin chưa biết cách setup, gửi file này cho họ:
📄 `FIREBASE_ADMIN_GUIDE.md`

## Kiểm tra nhanh

Sau khi có file `google-services.json`:

1. Mở file và tìm dòng:
```json
"package_name": "com.example.donvesinhcuanv"
```

2. Nếu package_name khác, đổi trong `app/build.gradle.kts`

3. Build và test!
