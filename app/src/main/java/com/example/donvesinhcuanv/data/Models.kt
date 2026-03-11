package com.example.donvesinhcuanv.data

import java.util.Date

// Model cho dịch vụ
data class Service(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val price: Int,
    val duration: Int, // phút
    val icon: String
)

// Model cho đơn hàng/công việc
data class Job(
    val id: String,
    val serviceId: String,
    val serviceName: String,
    val customerName: String,
    val customerPhone: String,
    val address: String,
    val distance: Double, // km
    val scheduledDate: Date,
    val price: Int,
    val status: JobStatus,
    val description: String,
    val icon: String,
    val isNew: Boolean = false // Đơn mới vừa tạo
)

enum class JobStatus {
    PENDING,      // Đang chờ nhân viên nhận
    ACCEPTED,     // Nhân viên đã nhận
    ARRIVING,     // Đang đến
    IN_PROGRESS,  // Đang thực hiện
    COMPLETED,    // Hoàn thành
    CANCELLED     // Đã hủy
}

// Model cho nhân viên
data class Worker(
    val id: String,
    val name: String,
    val email: String = "",
    val phone: String,
    val completedJobs: Int = 0,
    val averageRating: Double = 0.0,
    val totalEarnings: Int = 0,
    val todayEarnings: Int = 0,
    val isOnline: Boolean = false,
    val specialties: List<String> = emptyList(),
    val status: WorkerStatus = WorkerStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class WorkerStatus {
    PENDING,    // Chờ admin duyệt
    APPROVED,   // Đã được duyệt
    REJECTED    // Bị từ chối
}

// Model cho lịch sử công việc (Firestore)
data class JobHistoryItem(
    val jobId: String,
    val serviceName: String,
    val price: Int,
    val completedAt: Long,
    val scheduledDate: Long
)

// Model cho thống kê thu nhập (Firestore)
data class EarningsStats(
    val totalEarnings: Int,
    val totalJobs: Int,
    val averagePerJob: Int,
    val period: String // "today", "week", "month", "custom"
)
