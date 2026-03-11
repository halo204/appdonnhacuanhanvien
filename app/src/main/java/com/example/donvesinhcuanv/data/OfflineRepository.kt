package com.example.donvesinhcuanv.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository để test app offline không cần Firebase
 * Sử dụng dữ liệu mẫu từ SampleData
 */
class OfflineRepository {
    
    private var currentUser: Worker? = null
    private val pendingJobsList = SampleData.pendingJobs.toMutableList()
    private val acceptedJobsList = mutableListOf<Job>()
    
    // Auth
    fun getCurrentUser(): Worker? = currentUser
    
    suspend fun signUp(email: String, password: String, fullName: String, phone: String): Result<Worker> {
        return try {
            delay(1000) // Giả lập network delay
            
            val worker = Worker(
                id = "offline_${System.currentTimeMillis()}",
                name = fullName,
                email = email,
                phone = phone,
                completedJobs = 0,
                averageRating = 0.0,
                totalEarnings = 0,
                todayEarnings = 0,
                isOnline = false,
                specialties = listOf("Dọn dẹp nhà cửa", "Giặt ủi quần áo")
            )
            
            currentUser = worker
            Result.success(worker)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<Worker> {
        return try {
            delay(1000) // Giả lập network delay
            
            // Tạo user mẫu
            val worker = SampleData.currentWorker.copy(email = email)
            currentUser = worker
            Result.success(worker)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        currentUser = null
    }
    
    // Worker
    suspend fun getWorker(userId: String): Result<Worker> {
        return try {
            delay(500)
            Result.success(currentUser ?: SampleData.currentWorker)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateWorkerOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            delay(300)
            currentUser = currentUser?.copy(isOnline = isOnline)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateWorkerEarnings(userId: String, amount: Int): Result<Unit> {
        return try {
            delay(300)
            currentUser = currentUser?.copy(
                totalEarnings = (currentUser?.totalEarnings ?: 0) + amount,
                todayEarnings = (currentUser?.todayEarnings ?: 0) + amount,
                completedJobs = (currentUser?.completedJobs ?: 0) + 1
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Jobs
    fun getPendingJobs(): Flow<List<Job>> = flow {
        while (true) {
            emit(pendingJobsList.toList())
            delay(2000) // Update mỗi 2 giây
        }
    }
    
    fun getWorkerJobs(workerId: String): Flow<List<Job>> = flow {
        while (true) {
            emit(acceptedJobsList.toList())
            delay(2000)
        }
    }
    
    suspend fun acceptJob(jobId: String, workerId: String): Result<Unit> {
        return try {
            delay(500)
            val job = pendingJobsList.find { it.id == jobId }
            if (job != null) {
                pendingJobsList.remove(job)
                acceptedJobsList.add(job.copy(status = JobStatus.ACCEPTED, isNew = false))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateJobStatus(jobId: String, status: JobStatus): Result<Unit> {
        return try {
            delay(300)
            val index = acceptedJobsList.indexOfFirst { it.id == jobId }
            if (index != -1) {
                acceptedJobsList[index] = acceptedJobsList[index].copy(status = status)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun completeJob(jobId: String): Result<Unit> {
        return try {
            delay(500)
            acceptedJobsList.removeAll { it.id == jobId }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
