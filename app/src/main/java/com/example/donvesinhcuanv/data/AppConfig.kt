package com.example.donvesinhcuanv.data

/**
 * Cấu hình app
 * 
 * App đã chuyển sang dùng MySQL hoàn toàn
 */
object AppConfig {
    // App name
    const val APP_NAME = "CleanService Worker"
    const val VERSION = "1.0.0"
    
    // MySQL Server URL
    // Emulator: http://10.0.2.2:3000
    // Real device: http://YOUR_IP:3000
    const val API_BASE_URL = "http://10.0.2.2:3000/"
}
