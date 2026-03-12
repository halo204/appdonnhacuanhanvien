# Fix Google Play Services trên Emulator có Play Store

## Vấn đề

Emulator CÓ Google Play Store (`PlayStore.enabled: true`) nhưng vẫn lỗi:
```
PhoneType API is not available on this device
Connection failed with: ConnectionResult{statusCode=DEVELOPER_ERROR}
```

## Nguyên nhân

Google Play Services trên emulator:
- Chưa được cập nhật
- Chưa khởi động đầy đủ
- Bị lỗi cache

---

## Giải pháp 1: Update Google Play Services (Thử đầu tiên)

### Bước 1: Mở Emulator
1. Khởi động emulator **Pixel 8a API 36.0**
2. Đợi boot xong hoàn toàn

### Bước 2: Đăng nhập Google Account
1. Mở app **Settings**
2. **Accounts** → **Add account** → **Google**
3. Đăng nhập với Google account của bạn
4. Hoàn tất setup

### Bước 3: Update Google Play Services
1. Mở app **Play Store**
2. Tap vào avatar (góc trên bên phải)
3. **Manage apps & device**
4. Tab **Updates available**
5. Tìm **Google Play services**
6. Nếu có → Tap **Update**
7. Đợi update xong (có thể mất 2-3 phút)

### Bước 4: Restart Emulator
1. Đóng emulator
2. Mở lại emulator
3. Đợi boot xong
4. Run app và test lại

---

## Giải pháp 2: Cold Boot Emulator (Nếu vẫn lỗi)

### Bước 1: Đóng Emulator hiện tại
- Click nút X để đóng emulator

### Bước 2: Cold Boot
1. **Device Manager** → Click vào emulator **Pixel 8a**
2. Click dropdown ▼ bên cạnh nút Play
3. Chọn **Cold Boot Now**
4. Đợi emulator khởi động (lâu hơn bình thường)

### Bước 3: Test lại
- Run app
- Thử đăng ký

---

## Giải pháp 3: Wipe Data (Nếu vẫn lỗi)

⚠️ **Cảnh báo:** Sẽ xóa toàn bộ data trên emulator!

### Bước 1: Wipe Data
1. **Device Manager**
2. Click vào emulator **Pixel 8a**
3. Click dropdown ▼
4. Chọn **Wipe Data**
5. Confirm

### Bước 2: Khởi động lại
1. Start emulator
2. Setup lại (chọn ngôn ngữ, timezone, etc.)
3. Đăng nhập Google account
4. Đợi Google Play Services tự động update

### Bước 3: Test
- Run app
- Thử đăng ký

---

## Giải pháp 4: Tạo Emulator mới với API 34 (Khuyến nghị)

API 36.0 (Android 16) vẫn đang preview, chưa hoàn toàn stable.

### Bước 1: Create New Device
1. **Device Manager** → **Create Device**
2. Chọn **Pixel 8a** (hoặc Pixel 5)
3. Click **Next**

### Bước 2: Chọn System Image
1. Tab **Recommended**
2. Tìm: **API 34 (Android 14.0)** với icon **▶️ Google Play**
3. Click **Download** nếu chưa có
4. Chọn image đó
5. Click **Next**

### Bước 3: Finish
1. AVD Name: **Pixel 8a API 34**
2. Click **Finish**

### Bước 4: Run
1. Chọn emulator mới trong dropdown
2. Run app
3. Test đăng ký

---

## Giải pháp 5: Check Google Play Services trong Emulator

### Kiểm tra version:
1. Mở emulator
2. **Settings** → **Apps** → **See all apps**
3. Tìm **Google Play services**
4. Xem version (nên là 24.x.x trở lên)
5. Nếu quá cũ → Update qua Play Store

### Force Stop và Clear Cache:
1. **Settings** → **Apps** → **Google Play services**
2. **Force stop**
3. **Storage & cache** → **Clear cache**
4. Restart emulator
5. Test lại

---

## Giải pháp 6: Tạm thời disable Firestore (Workaround)

Nếu tất cả đều thất bại, tạm thời chỉ dùng Realtime Database:

### File: `FirebaseRepository.kt`

```kotlin
suspend fun signUp(email: String, password: String, fullName: String, phone: String): Result<FirebaseUser> {
    return try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("User creation failed")
        
        // Chỉ dùng Realtime Database
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
        
        // COMMENT TẠM THỜI - Firestore có vấn đề với emulator
        /*
        val timestamp = System.currentTimeMillis()
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

⚠️ **Lưu ý:** Đây chỉ là workaround tạm thời để test. Sau này cần uncomment lại!

---

## Giải pháp 7: Dùng Real Device (Tốt nhất)

Nếu có điện thoại Android thật:

### Bước 1: Enable Developer Options
1. **Settings** → **About phone**
2. Tap **Build number** 7 lần
3. Nhập PIN/Password

### Bước 2: Enable USB Debugging
1. **Settings** → **System** → **Developer options**
2. Bật **USB debugging**
3. Bật **Install via USB**

### Bước 3: Connect
1. Cắm USB vào máy tính
2. Trên điện thoại: Allow USB debugging
3. Android Studio: Chọn device trong dropdown
4. Run app

Real device luôn có Google Play Services đầy đủ và stable!

---

## Checklist

- [ ] Update Google Play Services trên emulator
- [ ] Restart emulator
- [ ] Nếu vẫn lỗi: Cold Boot
- [ ] Nếu vẫn lỗi: Wipe Data
- [ ] Nếu vẫn lỗi: Tạo emulator API 34
- [ ] Hoặc: Dùng real device
- [ ] Hoặc: Tạm thời disable Firestore

---

## Tại sao lỗi này xảy ra?

1. **API 36 (Android 16) là preview** - Chưa stable
2. **Google Play Services chưa được optimize** cho Android 16
3. **Emulator có thể thiếu một số components** của Play Services
4. **Firebase SDK chưa được test đầy đủ** với Android 16

**Khuyến nghị:** Dùng API 34 (Android 14) cho development, stable và đầy đủ tính năng!

---

## Kết luận

Emulator của bạn CÓ Google Play Store nhưng Google Play Services có vấn đề. Thử các giải pháp theo thứ tự:

1. ✅ Update Play Services → Restart
2. ✅ Cold Boot
3. ✅ Wipe Data
4. ⭐ Tạo emulator API 34 (Khuyến nghị)
5. ⭐⭐⭐ Dùng real device (Tốt nhất)

Sau khi làm một trong các cách trên, app sẽ chạy bình thường!
