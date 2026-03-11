# Fix lỗi Google Play Services

## Lỗi

```
Failed to get service from broker
java.lang.SecurityException: Unknown calling package name 'com.google.android.gms'
```

## Nguyên nhân

1. Emulator không có Google Play Services
2. Google Play Services bị outdated
3. SHA-1 fingerprint chưa được thêm vào Firebase

---

## Giải pháp

### 1. Dùng Emulator có Google Play (Recommended)

**Tạo emulator mới:**

1. Android Studio → **Device Manager**
2. Click **Create Device**
3. Chọn device (ví dụ: Pixel 5)
4. Chọn system image có **Play Store icon** (ví dụ: API 34 with Google Play)
5. Click **Next** → **Finish**
6. Run app trên emulator này

✅ Emulator có Play Store icon = có Google Play Services

---

### 2. Update Google Play Services trên Emulator

Nếu đang dùng emulator có Play Store:

1. Mở emulator
2. Mở **Play Store** app
3. Search "Google Play Services"
4. Click **Update** nếu có
5. Restart emulator
6. Run app lại

---

### 3. Thêm SHA-1 Fingerprint vào Firebase

Firebase cần SHA-1 để verify app:

**Lấy SHA-1:**

```bash
# Debug SHA-1
cd android
./gradlew signingReport

# Hoặc
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Output:**
```
SHA1: A1:B2:C3:D4:E5:F6:...
```

**Thêm vào Firebase:**

1. Vào Firebase Console
2. Project Settings → Your apps → Android app
3. Scroll xuống **SHA certificate fingerprints**
4. Click **Add fingerprint**
5. Paste SHA-1
6. Click **Save**
7. Download `google-services.json` mới
8. Replace file cũ trong `app/`
9. Sync Gradle
10. Clean & Rebuild

---

### 4. Test trên Real Device (Nhanh nhất)

Nếu có điện thoại Android thật:

1. Enable **Developer Options**:
   - Settings → About phone
   - Tap **Build number** 7 lần
   
2. Enable **USB Debugging**:
   - Settings → Developer options
   - Turn on **USB debugging**

3. Connect phone qua USB

4. Run app từ Android Studio

✅ Real device luôn có Google Play Services!

---

### 5. Dùng Test Mode (Tạm thời)

Nếu chỉ muốn test UI không cần Firebase:

**Tắt Firebase tạm thời:**

File: `app/src/main/java/com/example/donvesinhcuanv/data/AppConfig.kt`

```kotlin
object AppConfig {
    const val USE_FIREBASE = false  // Đổi thành false
    const val USE_OFFLINE_DATA = true
}
```

Rebuild app → Sẽ dùng offline data thay vì Firebase.

---

## Kiểm tra Google Play Services

### Trong code:

```kotlin
import com.google.android.gms.common.GoogleApiAvailability

fun checkGooglePlayServices(context: Context): Boolean {
    val googleApiAvailability = GoogleApiAvailability.getInstance()
    val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
    
    return when (resultCode) {
        ConnectionResult.SUCCESS -> {
            Log.d("PlayServices", "Google Play Services available")
            true
        }
        else -> {
            Log.e("PlayServices", "Google Play Services not available: $resultCode")
            
            // Show dialog to update
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(
                    context as Activity,
                    resultCode,
                    9000
                )?.show()
            }
            false
        }
    }
}
```

### Trong MainActivity:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Check Google Play Services
    if (!checkGooglePlayServices(this)) {
        Toast.makeText(
            this,
            "Google Play Services not available. Some features may not work.",
            Toast.LENGTH_LONG
        ).show()
    }
    
    // ... rest of code
}
```

---

## Emulator Recommendations

### ✅ Recommended (có Google Play):
- Pixel 5 - API 34 (Android 14) with Google Play
- Pixel 4 - API 33 (Android 13) with Google Play
- Pixel 3a - API 30 (Android 11) with Google Play

### ❌ Không dùng (không có Google Play):
- Any emulator without Play Store icon
- AOSP images
- Google APIs (without Play Store)

---

## Troubleshooting

### Lỗi vẫn còn sau khi thêm SHA-1

**Giải pháp:**
1. Xóa app khỏi emulator
2. Clean project: Build → Clean Project
3. Rebuild: Build → Rebuild Project
4. Run lại

### Emulator quá chậm

**Giải pháp:**
1. Giảm RAM: 2GB là đủ
2. Enable Hardware Acceleration
3. Hoặc dùng real device

### SHA-1 không match

**Giải pháp:**
1. Chắc chắn dùng debug keystore
2. Path: `~/.android/debug.keystore`
3. Password: `android`
4. Alias: `androiddebugkey`

---

## Quick Fix (1 phút)

**Cách nhanh nhất:**

1. Tạo emulator mới có **Play Store icon**
2. Hoặc test trên **real device**
3. Hoặc tắt Firebase tạm thời (`USE_FIREBASE = false`)

---

## Production Build

Khi build production (release APK):

1. Tạo release keystore:
```bash
keytool -genkey -v -keystore release.keystore -alias mykey -keyalg RSA -keysize 2048 -validity 10000
```

2. Lấy SHA-1 của release keystore:
```bash
keytool -list -v -keystore release.keystore -alias mykey
```

3. Thêm SHA-1 này vào Firebase

4. Update `build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "your_password"
            keyAlias = "mykey"
            keyPassword = "your_password"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## Summary

✅ **Best solution:** Dùng emulator có Play Store hoặc real device

✅ **Quick fix:** Tắt Firebase tạm thời để test UI

✅ **Production:** Thêm SHA-1 của release keystore vào Firebase

❌ **Không dùng:** Emulator không có Google Play Services
