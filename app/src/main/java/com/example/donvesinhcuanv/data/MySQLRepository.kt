package com.example.donvesinhcuanv.data

import com.example.donvesinhcuanv.data.api.ApiService
import com.example.donvesinhcuanv.data.api.LoginRequest
import com.example.donvesinhcuanv.data.api.RegisterRequest
import com.example.donvesinhcuanv.data.api.RetrofitClient
import com.example.donvesinhcuanv.data.api.UpdateJobRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MySQLRepository {
    private val apiService: ApiService = RetrofitClient.apiService
    private var currentWorker: Worker? = null
    private var authToken: String? = null
    
    // Auth
    suspend fun signUp(email: String, password: String, fullName: String, phone: String): Result<Worker> {
        return try {
            val request = RegisterRequest(
                name = fullName,
                email = email,
                password = password,
                phone = phone
            )
            
            val response = apiService.register(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val worker = response.body()?.worker
                val token = response.body()?.token
                
                if (worker != null) {
                    currentWorker = worker
                    authToken = token
                    Result.success(worker)
                } else {
                    Result.failure(Exception("Đăng ký thất bại"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "Đăng ký thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<Worker> {
        return try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val worker = response.body()?.worker
                val token = response.body()?.token
                
                if (worker != null) {
                    currentWorker = worker
                    authToken = token
                    Result.success(worker)
                } else {
                    Result.failure(Exception("Đăng nhập thất bại"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "Đăng nhập thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        currentWorker = null
        authToken = null
    }
    
    fun getCurrentWorker(): Worker? = currentWorker
    
    fun isLoggedIn(): Boolean = currentWorker != null
    
    // Worker
    suspend fun getWorker(workerId: String): Result<Worker> {
        return try {
            val response = apiService.getWorker(workerId)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Không tìm thấy worker"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateWorkerOnlineStatus(workerId: String, isOnline: Boolean): Result<Unit> {
        return try {
            val response = apiService.updateOnlineStatus(workerId, mapOf("isOnline" to isOnline))
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Cập nhật trạng thái thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Jobs
    fun getPendingJobs(): Flow<List<Job>> = flow {
        try {
            val response = apiService.getPendingJobs()
            
            if (response.isSuccessful && response.body()?.success == true) {
                emit(response.body()?.jobs ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    fun getWorkerJobs(workerId: String): Flow<List<Job>> = flow {
        try {
            val response = apiService.getWorkerJobs(workerId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                emit(response.body()?.jobs ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    suspend fun acceptJob(jobId: String, workerId: String): Result<Unit> {
        return try {
            val request = UpdateJobRequest(status = "ACCEPTED", workerId = workerId)
            val response = apiService.acceptJob(jobId, request)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Nhận đơn thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateJobStatus(jobId: String, status: JobStatus): Result<Unit> {
        return try {
            val request = UpdateJobRequest(status = status.name)
            val response = apiService.updateJobStatus(jobId, request)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Cập nhật trạng thái thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun completeJob(jobId: String): Result<Unit> {
        return try {
            val response = apiService.completeJob(jobId)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Hoàn thành đơn thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
