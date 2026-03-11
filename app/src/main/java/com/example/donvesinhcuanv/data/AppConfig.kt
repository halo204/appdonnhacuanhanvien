package com.example.donvesinhcuanv.data

/**
 * Cấu hình app
 * 
 * Đổi USE_FIREBASE = false để chạy offline mode (không cần Firebase)
 * Đổi USE_FIREBASE = true để kết nối Firebase thật
 */
object AppConfig {
    // Đổi thành true để kết nối Firebase
    const val USE_FIREBASE = true  // Bật Firebase để đăng ký/đăng nhập
    
    // Thông tin app
    const val APP_NAME = "CleanService Worker"
    const val VERSION = "1.0.0"
}
