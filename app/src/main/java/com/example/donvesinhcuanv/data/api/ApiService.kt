package com.example.donvesinhcuanv.data.api

import com.example.donvesinhcuanv.data.Worker
import com.example.donvesinhcuanv.data.Job
import retrofit2.Response
import retrofit2.http.*

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val worker: Worker? = null,
    val token: String? = null
)

data class JobsResponse(
    val success: Boolean,
    val jobs: List<Job>
)

data class UpdateJobRequest(
    val status: String,
    val workerId: String? = null
)

interface ApiService {
    // Auth
    @POST("api/workers/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("api/workers/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    // Worker
    @GET("api/workers/{id}")
    suspend fun getWorker(@Path("id") workerId: String): Response<Worker>
    
    @PUT("api/workers/{id}/online")
    suspend fun updateOnlineStatus(
        @Path("id") workerId: String,
        @Body status: Map<String, Boolean>
    ): Response<Unit>
    
    // Jobs
    @GET("api/jobs/pending")
    suspend fun getPendingJobs(): Response<JobsResponse>
    
    @GET("api/jobs/worker/{workerId}")
    suspend fun getWorkerJobs(@Path("workerId") workerId: String): Response<JobsResponse>
    
    @PUT("api/jobs/{jobId}/accept")
    suspend fun acceptJob(
        @Path("jobId") jobId: String,
        @Body request: UpdateJobRequest
    ): Response<Unit>
    
    @PUT("api/jobs/{jobId}/status")
    suspend fun updateJobStatus(
        @Path("jobId") jobId: String,
        @Body request: UpdateJobRequest
    ): Response<Unit>
    
    @PUT("api/jobs/{jobId}/complete")
    suspend fun completeJob(@Path("jobId") jobId: String): Response<Unit>
}
