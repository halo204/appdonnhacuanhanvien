package com.example.donvesinhcuanv.data

import java.util.Calendar
import java.util.Date

object SampleData {
    
    // Dữ liệu mẫu cho nhân viên
    val currentWorker = Worker(
        id = "W001",
        name = "Nguyễn Văn Test",
        email = "user@example.com",
        phone = "0909123456",
        completedJobs = 10,
        averageRating = 4.8,
        totalEarnings = 1500000,
        todayEarnings = 0,
        isOnline = false,
        specialties = listOf("Dọn dẹp nhà cửa", "Giặt ủi quần áo")
    )
    
    // Dữ liệu mẫu cho các dịch vụ
    val services = listOf(
        Service(
            id = "S001",
            name = "Dọn dẹp nhà cửa",
            category = "cleaning",
            description = "Dọn dẹp toàn bộ nhà cửa, lau chùi, hút bụi, sắp xếp đồ đạc gọn gàng",
            price = 150000,
            duration = 120,
            icon = "🏠"
        ),
        Service(
            id = "S002",
            name = "Giặt ủi quần áo",
            category = "laundry",
            description = "Giặt và ủi quần áo chuyên nghiệp, sử dụng hóa chất an toàn",
            price = 100000,
            duration = 90,
            icon = "👔"
        ),
        Service(
            id = "S003",
            name = "Vệ sinh máy lạnh",
            category = "maintenance",
            description = "Vệ sinh và bảo dưỡng máy lạnh, kiểm tra gas, làm sạch dàn nóng",
            price = 200000,
            duration = 60,
            icon = "❄️"
        )
    )
    
    // Tạo ngày giờ mẫu
    private fun createDate(daysFromNow: Int, hour: Int, minute: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, daysFromNow)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        return calendar.time
    }
    
    // Dữ liệu mẫu cho công việc đang chờ
    val pendingJobs = mutableListOf(
        Job(
            id = "J001",
            serviceId = "S001",
            serviceName = "Dọn dẹp nhà cửa",
            customerName = "Nguyễn Văn An",
            customerPhone = "0901234567",
            address = "123 Nguyễn Huệ, Q1, TP.HCM",
            distance = 2.5,
            scheduledDate = createDate(0, 10, 0),
            price = 150000,
            status = JobStatus.PENDING,
            description = "Dọn dẹp toàn bộ nhà cửa, lau chùi, hút bụi, sắp xếp đồ đạc gọn gàng",
            icon = "🏠",
            isNew = true
        ),
        Job(
            id = "J002",
            serviceId = "S002",
            serviceName = "Giặt ủi quần áo",
            customerName = "Trần Thị Bình",
            customerPhone = "0903456789",
            address = "456 Lê Lợi, Q3, TP.HCM",
            distance = 3.8,
            scheduledDate = createDate(0, 14, 0),
            price = 100000,
            status = JobStatus.PENDING,
            description = "Giặt và ủi quần áo chuyên nghiệp, sử dụng hóa chất an toàn",
            icon = "👔",
            isNew = false
        ),
        Job(
            id = "J003",
            serviceId = "S003",
            serviceName = "Vệ sinh máy lạnh",
            customerName = "Lê Văn Cường",
            customerPhone = "0904567890",
            address = "789 Võ Văn Tần, Q3, TP.HCM",
            distance = 1.2,
            scheduledDate = createDate(0, 15, 30),
            price = 200000,
            status = JobStatus.PENDING,
            description = "Vệ sinh và bảo dưỡng máy lạnh, kiểm tra gas, làm sạch dàn nóng",
            icon = "❄️",
            isNew = false
        )
    )
    
    val acceptedJobs = mutableListOf<Job>()
    val completedJobs = mutableListOf<Job>()
}
