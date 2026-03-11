# CleanService Worker App

Ứng dụng dành cho nhân viên dọn vệ sinh - Nhận và xử lý công việc từ khách hàng (giống Grab/Be)

## Tính năng chính

✅ Đăng nhập / Đăng ký
✅ Toggle Online/Offline để nhận đơn
✅ Xem danh sách đơn hàng mới real-time
✅ Nhận đơn và theo dõi trạng thái
✅ Cập nhật trạng thái: Đã nhận → Đang đến → Đang làm → Hoàn thành
✅ Theo dõi thu nhập (hôm nay & tổng)
✅ Hồ sơ nhân viên với rating

## Chế độ hoạt động

App hỗ trợ 2 chế độ:

### 1. Offline Mode (Mặc định - Không cần Firebase)
- Sử dụng dữ liệu mẫu
- Test app ngay lập tức
- Không cần cấu hình gì thêm

### 2. Firebase Mode (Kết nối thật)
- Cần cấu hình Firebase
- Lưu trữ dữ liệu thật
- Real-time updates

## Cách chuyển đổi chế độ

Mở file `app/src/main/java/com/example/donvesinhcuanv/data/AppConfig.kt`:

```kotlin
object AppConfig {
    // false = Offline mode (test)
    // true = Firebase mode (production)
    const val USE_FIREBASE = false
}
```

## Build và chạy app

### Offline Mode (Không cần Firebase)
```bash
./gradlew assembleDebug
```

### Firebase Mode
1. Làm theo hướng dẫn trong file `FIREBASE_SETUP.md`
2. Tải file `google-services.json` từ Firebase Console
3. Đặt file vào thư mục `app/`
4. Đổi `USE_FIREBASE = true` trong `AppConfig.kt`
5. Build:
```bash
./gradlew clean assembleDebug
```

## Cấu trúc project

```
app/
├── src/main/java/com/example/donvesinhcuanv/
│   ├── data/
│   │   ├── Models.kt              # Data models
│   │   ├── SampleData.kt          # Dữ liệu mẫu
│   │   ├── AppConfig.kt           # Cấu hình app
│   │   ├── FirebaseRepository.kt  # Firebase operations
│   │   └── OfflineRepository.kt   # Offline operations
│   ├── screens/
│   │   ├── LoginScreen.kt         # Màn hình đăng nhập
│   │   ├── RegisterScreen.kt      # Màn hình đăng ký
│   │   ├── HomeScreen.kt          # Trang chủ - Xem đơn mới
│   │   ├── MyJobsScreen.kt        # Đơn đang làm
│   │   ├── JobListScreen.kt       # Tất cả đơn
│   │   └── ProfileScreen.kt       # Hồ sơ
│   ├── viewmodel/
│   │   ├── AuthViewModel.kt       # Quản lý auth
│   │   └── JobViewModel.kt        # Quản lý jobs
│   ├── navigation/
│   │   └── NavGraph.kt            # Navigation
│   └── MainActivity.kt
└── google-services.json           # Firebase config (cần thay thế)
```

## Tài khoản test (Offline mode)

- Email: `user@example.com`
- Password: Bất kỳ (không kiểm tra)

## Lưu ý

- ⚠️ File `google-services.json` hiện tại là file mẫu, không hoạt động
- ⚠️ Để kết nối Firebase thật, xem hướng dẫn trong `FIREBASE_SETUP.md`
- ⚠️ Offline mode chỉ để test, dữ liệu sẽ mất khi tắt app

## Troubleshooting

### Build lâu
- Lần đầu build sẽ lâu do tải dependencies
- Các lần sau sẽ nhanh hơn

### Lỗi "API key not valid"
- Bạn đang dùng Firebase mode với file `google-services.json` mẫu
- Giải pháp: Đổi sang Offline mode hoặc cấu hình Firebase đúng

### App crash khi mở
- Kiểm tra đã build thành công chưa
- Xem logs trong Android Studio

## Liên hệ

Nếu cần hỗ trợ, tham khảo:
- `FIREBASE_SETUP.md` - Hướng dẫn cấu hình Firebase chi tiết
- Firebase Documentation: https://firebase.google.com/docs
