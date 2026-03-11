# Fix lỗi: Emulator không có Google Play Services

## Vấn đề

Logcat hiển thị:
```
PhoneType API is not available on this device
Connection failed with: ConnectionResult{statusCode=DEVELOPER_ERROR, resolution=null}
```

Nguyên nhân: Emulator bạn đang dùng KHÔNG có Google Play Services.

---

## Giải pháp 1: Tạo Emulator mới có Google Play (KHUYẾN NGHỊ)

### Bước 1: Mở AVD Manager

1. Android Studio → **Tools** → **Device Manager**
2. Hoặc click icon điện thoại ở thanh toolbar

### Bước 2: Tạo Virtual Device mới

1. Click **Create Device**
2. Chọn device: **Pixel 5** hoặc **Pixel 6** (recommended)
3. Click **Next**

### Bước 3: Chọn System Image CÓ Google Play

**QUAN TRỌNG:** Phải chọn image có biểu tượng **Google Play** (hình tam giác màu)

Chọn một trong các options sau:

**Option 1: Android 14 (API 34) - Recommended**
- Tab: **Recommended**
- Tìm: **UpsideDownCake** hoặc **API Level 34**
- Chọn dòng có icon **Google Play** (▶️)
- Click **Download** nếu chưa có
- Sau khi download xong, chọn image đó

**Option 2: Android 13 (API 33)**
- Tab: **Recommended**
- Tìm: **Tiramisu** hoặc **API Level 33**
- Chọn dòng có icon **Google Play**

**Option 3: Android 11 (API 30)**
- Tab: **x86 Images**
- Tìm: **R** hoặc **API Level 30**
- Chọn dòng có icon **Google Play**

⚠️ **TRÁNH:** Không chọn dòng có chữ "Google APIs" (không có Play Store icon)

### Bước 4: Finish

1. Click **Next**
2. Đặt tên: **Pixel 5 API 34 (Play Store)**
3. Click **Finish**

### Bước 5: Run app trên emulator mới

1. Chọn emulator mới trong dropdown
2. Click **Run** (▶️)
3. Đợi emulator khởi động
4. Test đăng ký lại

---

## Giải pháp 2: Dùng Real Device (Nhanh nhất)

### Bước 1: Enable Developer Options

1. Trên điện thoại Android: **Settings** → **About phone**
2. Tap **Build number** 7 lần
3. Nhập PIN/Password
4. Thấy thông báo "You are now a developer"

### Bước 2: Enable USB Debugging

1. **Settings** → **System** → **Developer options**
2. Bật **USB debugging**
3. Bật **Install via USB** (nếu có)

### Bước 3: Kết nối với máy tính

1. Cắm USB cable
2. Trên điện thoại: Chọn **File Transfer** hoặc **PTP**
3. Allow USB debugging popup

### Bước 4: Run app

1. Android Studio: Chọn device trong dropdown
2. Click **Run** (▶️)
3. App sẽ được cài lên điện thoại thật

---

## Giải pháp 3: Sửa code để không cần Google Play Services (Tạm thời)

Nếu không thể tạo emulator mới hoặc dùng real device, tạm thời comment code liên quan đến Firestore:

### File: `FirebaseRepository.kt`

```kotlin
suspend fun signUp(email: String, password: String, fullName: String, phone: String): Result<FirebaseUser> {
    return try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("User creation failed")
        
        val timestamp = System.currentTimeMillis()
        
        // Tạo worker profile trong Realtime Database (cho real-time status)
        val realtimeWorker = hashMapOf(
            "id" to user.uid,
            "name" to fullName,
            "email" to email,
            "phone" to phone,
            "completedJobs" to 0,
            "averageRating" to 0.0,
            "totalEarnings" to 0,
            "todayEarnings" to 0,
            "isOnline" to false,
            "status" to WorkerStatus.PENDING.name,
            "createdAt" to ServerValue.TIMESTAMP
        )
        workersRef.child(user.uid).setValue(realtimeWorker).await()
        
        // COMMENT TẠM THỜI - Firestore cần Google Play Services
        /*
        val firestoreWorker = hashMapOf(
            "id" to user.uid,
            "name" to fullName,
            "email" to email,
            "phone" to phone,
            "completedJobs" to 0,
            "averageRating" to 0.0,
            "totalEarnings" to 0,
            "todayEarnings" to 0,
            "status" to WorkerStatus.PENDING.name,
            "specialties" to emptyList<String>(),
            "createdAt" to timestamp,
            "updatedAt" to timestamp
        )
        workersCollection.document(user.uid).set(firestoreWorker).await()
        */
        
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

⚠️ **LƯU Ý:** Đây chỉ là giải pháp tạm thời để test. Sau này phải uncomment lại!

---

## Kiểm tra Emulator có Google Play không

### Cách 1: Xem trong AVD Manager

1. **Device Manager**
2. Xem cột **Play Store**
3. Nếu có icon ▶️ → Có Google Play
4. Nếu trống → KHÔNG có Google Play

### Cách 2: Mở emulator và kiểm tra

1. Mở emulator
2. Tìm app **Play Store** trong app drawer
3. Nếu có → Có Google Play
4. Nếu không có → KHÔNG có Google Play

---

## So sánh các loại System Images

| Type | Icon | Google Play | Firebase | Khuyến nghị |
|------|------|-------------|----------|-------------|
| **Google Play** | ▶️ | ✅ Có | ✅ Đầy đủ | ⭐⭐⭐⭐⭐ Dùng cái này |
| **Google APIs** | 🔧 | ❌ Không | ⚠️ Hạn chế | ⭐⭐ Chỉ test API |
| **AOSP** | 📱 | ❌ Không | ❌ Không | ⭐ Không dùng |

---

## Emulator Recommended Settings

Khi tạo emulator mới:

```
Device: Pixel 5 hoặc Pixel 6
System Image: API 34 (Android 14) với Google Play
RAM: 2048 MB (tối thiểu)
Internal Storage: 2048 MB
SD Card: 512 MB (optional)
Graphics: Automatic hoặc Hardware
```

---

## Troubleshooting

### Lỗi: "Google Play services is updating"

**Giải pháp:**
1. Đợi emulator update xong (2-3 phút)
2. Restart emulator
3. Test lại

### Lỗi: "This app won't run without Google Play services"

**Giải pháp:**
1. Mở **Play Store** trên emulator
2. Search "Google Play services"
3. Update nếu có
4. Restart emulator

### Emulator quá chậm

**Giải pháp:**
1. Tăng RAM: AVD Manager → Edit → Advanced → RAM: 4096 MB
2. Enable Hardware Acceleration:
   - Windows: Intel HAXM
   - Mac: Hypervisor.framework
   - Linux: KVM
3. Chọn Graphics: Hardware - GLES 2.0

### Không thấy option "Google Play" khi tạo emulator

**Nguyên nhân:** CPU không hỗ trợ hoặc chưa enable virtualization

**Giải pháp:**
1. Vào BIOS
2. Enable Intel VT-x hoặc AMD-V
3. Restart máy tính
4. Thử lại

---

## Checklist

- [ ] Tạo emulator mới với Google Play System Image
- [ ] Hoặc dùng real device với USB debugging
- [ ] Hoặc comment Firestore code tạm thời
- [ ] Run app trên device/emulator mới
- [ ] Test đăng ký
- [ ] ✅ Không còn lỗi "PhoneType API not available"

---

## Kết luận

Lỗi PERMISSION_DENIED của bạn KHÔNG PHẢI do Firebase Rules, mà do:

1. ❌ Emulator không có Google Play Services
2. ❌ Firebase SDK cần Google Play Services để hoạt động
3. ✅ Giải pháp: Dùng emulator có Google Play hoặc real device

Sau khi đổi emulator/device, app sẽ chạy bình thường!
