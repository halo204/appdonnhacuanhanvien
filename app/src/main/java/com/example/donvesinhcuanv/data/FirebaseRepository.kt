package com.example.donvesinhcuanv.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    
    // References
    private val workersRef = database.getReference("workers")
    private val jobsRef = database.getReference("jobs")
    
    // Auth
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    suspend fun signUp(email: String, password: String, fullName: String, phone: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")
            
            // Tạo worker profile
            val worker = hashMapOf(
                "id" to user.uid,
                "name" to fullName,
                "email" to email,
                "phone" to phone,
                "completedJobs" to 0,
                "averageRating" to 0.0,
                "totalEarnings" to 0,
                "todayEarnings" to 0,
                "isOnline" to false,
                "createdAt" to ServerValue.TIMESTAMP
            )
            
            workersRef.child(user.uid).setValue(worker).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    // Worker
    suspend fun getWorker(userId: String): Result<Worker> {
        return try {
            val snapshot = workersRef.child(userId).get().await()
            
            if (!snapshot.exists()) {
                return Result.failure(Exception("Worker not found"))
            }
            
            val worker = Worker(
                id = snapshot.child("id").getValue(String::class.java) ?: userId,
                name = snapshot.child("name").getValue(String::class.java) ?: "",
                email = snapshot.child("email").getValue(String::class.java) ?: "",
                phone = snapshot.child("phone").getValue(String::class.java) ?: "",
                completedJobs = snapshot.child("completedJobs").getValue(Int::class.java) ?: 0,
                averageRating = snapshot.child("averageRating").getValue(Double::class.java) ?: 0.0,
                totalEarnings = snapshot.child("totalEarnings").getValue(Int::class.java) ?: 0,
                todayEarnings = snapshot.child("todayEarnings").getValue(Int::class.java) ?: 0,
                isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false,
                specialties = (snapshot.child("specialties").value as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                status = try {
                    WorkerStatus.valueOf(snapshot.child("status").getValue(String::class.java) ?: "PENDING")
                } catch (e: Exception) {
                    WorkerStatus.PENDING
                },
                createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            )
            Result.success(worker)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateWorkerOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            workersRef.child(userId).child("isOnline").setValue(isOnline).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateWorkerEarnings(userId: String, amount: Int): Result<Unit> {
        return try {
            val workerRef = workersRef.child(userId)
            
            // Get current values
            val snapshot = workerRef.get().await()
            val currentTotal = snapshot.child("totalEarnings").getValue(Int::class.java) ?: 0
            val currentToday = snapshot.child("todayEarnings").getValue(Int::class.java) ?: 0
            val completedJobs = snapshot.child("completedJobs").getValue(Int::class.java) ?: 0
            
            // Update values
            val updates = hashMapOf<String, Any>(
                "totalEarnings" to (currentTotal + amount),
                "todayEarnings" to (currentToday + amount),
                "completedJobs" to (completedJobs + 1)
            )
            
            workerRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Jobs
    fun getPendingJobs(): Flow<List<Job>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jobs = mutableListOf<Job>()
                for (jobSnapshot in snapshot.children) {
                    try {
                        val status = jobSnapshot.child("status").getValue(String::class.java)
                        if (status == "PENDING") {
                            val job = Job(
                                id = jobSnapshot.key ?: "",
                                serviceId = jobSnapshot.child("serviceId").getValue(String::class.java) ?: "",
                                serviceName = jobSnapshot.child("serviceName").getValue(String::class.java) ?: "",
                                customerName = jobSnapshot.child("customerName").getValue(String::class.java) ?: "",
                                customerPhone = jobSnapshot.child("customerPhone").getValue(String::class.java) ?: "",
                                address = jobSnapshot.child("customerAddress").getValue(String::class.java) ?: "",
                                distance = jobSnapshot.child("distance").getValue(Double::class.java) ?: 0.0,
                                scheduledDate = java.util.Date(jobSnapshot.child("scheduledDate").getValue(Long::class.java) ?: System.currentTimeMillis()),
                                price = jobSnapshot.child("price").getValue(Int::class.java) ?: 0,
                                status = JobStatus.valueOf(status),
                                description = jobSnapshot.child("description").getValue(String::class.java) ?: "",
                                icon = jobSnapshot.child("serviceIcon").getValue(String::class.java) ?: "🏠",
                                isNew = jobSnapshot.child("isNew").getValue(Boolean::class.java) ?: false
                            )
                            jobs.add(job)
                        }
                    } catch (e: Exception) {
                        // Skip invalid jobs
                    }
                }
                // Sort by createdAt
                jobs.sortBy { it.scheduledDate }
                trySend(jobs)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        jobsRef.orderByChild("status").equalTo("PENDING").addValueEventListener(listener)
        
        awaitClose { jobsRef.removeEventListener(listener) }
    }
    
    fun getWorkerJobs(workerId: String): Flow<List<Job>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jobs = mutableListOf<Job>()
                for (jobSnapshot in snapshot.children) {
                    try {
                        val jobWorkerId = jobSnapshot.child("workerId").getValue(String::class.java)
                        val status = jobSnapshot.child("status").getValue(String::class.java)
                        
                        if (jobWorkerId == workerId && status in listOf("ACCEPTED", "ARRIVING", "IN_PROGRESS")) {
                            val job = Job(
                                id = jobSnapshot.key ?: "",
                                serviceId = jobSnapshot.child("serviceId").getValue(String::class.java) ?: "",
                                serviceName = jobSnapshot.child("serviceName").getValue(String::class.java) ?: "",
                                customerName = jobSnapshot.child("customerName").getValue(String::class.java) ?: "",
                                customerPhone = jobSnapshot.child("customerPhone").getValue(String::class.java) ?: "",
                                address = jobSnapshot.child("customerAddress").getValue(String::class.java) ?: "",
                                distance = jobSnapshot.child("distance").getValue(Double::class.java) ?: 0.0,
                                scheduledDate = java.util.Date(jobSnapshot.child("scheduledDate").getValue(Long::class.java) ?: System.currentTimeMillis()),
                                price = jobSnapshot.child("price").getValue(Int::class.java) ?: 0,
                                status = JobStatus.valueOf(status ?: "PENDING"),
                                description = jobSnapshot.child("description").getValue(String::class.java) ?: "",
                                icon = jobSnapshot.child("serviceIcon").getValue(String::class.java) ?: "🏠",
                                isNew = false
                            )
                            jobs.add(job)
                        }
                    } catch (e: Exception) {
                        // Skip invalid jobs
                    }
                }
                trySend(jobs)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        jobsRef.addValueEventListener(listener)
        
        awaitClose { jobsRef.removeEventListener(listener) }
    }
    
    suspend fun acceptJob(jobId: String, workerId: String): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to "ACCEPTED",
                "workerId" to workerId,
                "acceptedAt" to ServerValue.TIMESTAMP,
                "isNew" to false
            )
            jobsRef.child(jobId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateJobStatus(jobId: String, status: JobStatus): Result<Unit> {
        return try {
            jobsRef.child(jobId).child("status").setValue(status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun completeJob(jobId: String): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to "COMPLETED",
                "completedAt" to ServerValue.TIMESTAMP
            )
            jobsRef.child(jobId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Phone Auth methods
    suspend fun sendOtp(
        phoneNumber: String,
        activity: android.app.Activity,
        onCodeSent: (String) -> Unit,
        onVerificationCompleted: (com.google.firebase.auth.PhoneAuthCredential) -> Unit,
        onVerificationFailed: (com.google.firebase.FirebaseException) -> Unit
    ) {
        val formattedPhone = if (phoneNumber.startsWith("+84")) {
            phoneNumber
        } else {
            "+84${phoneNumber.removePrefix("0")}"
        }
        
        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    onVerificationCompleted(credential)
                }
                
                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    onVerificationFailed(e)
                }
                
                override fun onCodeSent(
                    verificationId: String,
                    token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
                ) {
                    onCodeSent(verificationId)
                }
            })
            .build()
        
        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    suspend fun verifyOtp(verificationId: String, otpCode: String): Result<FirebaseUser> {
        return try {
            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, otpCode)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Sign in failed")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signInWithCredential(credential: com.google.firebase.auth.PhoneAuthCredential): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Sign in failed")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Storage methods - REMOVED (no image upload needed)
    
    suspend fun createWorkerProfile(
        userId: String,
        name: String,
        phone: String
    ): Result<Unit> {
        return try {
            val worker = hashMapOf(
                "id" to userId,
                "name" to name,
                "phone" to phone,
                "email" to "",
                "completedJobs" to 0,
                "averageRating" to 0.0,
                "totalEarnings" to 0,
                "todayEarnings" to 0,
                "isOnline" to false,
                "status" to WorkerStatus.PENDING.name,
                "createdAt" to ServerValue.TIMESTAMP
            )
            
            workersRef.child(userId).setValue(worker).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeWorkerStatus(userId: String): Flow<WorkerStatus> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java)
                val workerStatus = try {
                    WorkerStatus.valueOf(status ?: "PENDING")
                } catch (e: Exception) {
                    WorkerStatus.PENDING
                }
                trySend(workerStatus)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        workersRef.child(userId).addValueEventListener(listener)
        
        awaitClose { workersRef.child(userId).removeEventListener(listener) }
    }
}
