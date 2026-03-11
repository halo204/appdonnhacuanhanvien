package com.example.donvesinhcuanv.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    
    // Realtime Database - cho real-time updates
    private val database = FirebaseDatabase.getInstance()
    private val workersRef = database.getReference("workers")
    private val jobsRef = database.getReference("jobs")
    
    // Firestore - cho queries phức tạp và analytics
    private val firestore = FirebaseFirestore.getInstance()
    private val workersCollection = firestore.collection("workers")
    private val jobsCollection = firestore.collection("jobs")
    private val jobHistoryCollection = firestore.collection("jobHistory")
    private val analyticsCollection = firestore.collection("analytics")
    
    // Auth
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    suspend fun signUp(email: String, password: String, fullName: String, phone: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")
            
            val timestamp = System.currentTimeMillis()
            
            // Tạo worker profile trong Realtime Database (cho real-time status)
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
                "createdAt" to ServerValue.TIMESTAMP
            )
            workersRef.child(user.uid).setValue(realtimeWorker).await()
            
            // Tạo worker profile trong Firestore (cho queries và analytics)
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
            // Get current values from Realtime DB
            val snapshot = workersRef.child(userId).get().await()
            val currentTotal = snapshot.child("totalEarnings").getValue(Int::class.java) ?: 0
            val currentToday = snapshot.child("todayEarnings").getValue(Int::class.java) ?: 0
            val completedJobs = snapshot.child("completedJobs").getValue(Int::class.java) ?: 0
            
            val newTotal = currentTotal + amount
            val newToday = currentToday + amount
            val newCompleted = completedJobs + 1
            
            // Update Realtime Database
            val realtimeUpdates = hashMapOf<String, Any>(
                "totalEarnings" to newTotal,
                "todayEarnings" to newToday,
                "completedJobs" to newCompleted
            )
            workersRef.child(userId).updateChildren(realtimeUpdates).await()
            
            // Update Firestore
            val firestoreUpdates = hashMapOf<String, Any>(
                "totalEarnings" to newTotal,
                "todayEarnings" to newToday,
                "completedJobs" to newCompleted,
                "updatedAt" to System.currentTimeMillis()
            )
            workersCollection.document(userId).update(firestoreUpdates).await()
            
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
            val timestamp = System.currentTimeMillis()
            
            // Update Realtime Database
            val realtimeUpdates = hashMapOf<String, Any>(
                "status" to "COMPLETED",
                "completedAt" to ServerValue.TIMESTAMP
            )
            jobsRef.child(jobId).updateChildren(realtimeUpdates).await()
            
            // Get job data để lưu vào history
            val jobSnapshot = jobsRef.child(jobId).get().await()
            
            // Lưu vào Firestore job history (cho analytics và báo cáo)
            val jobHistory = hashMapOf(
                "jobId" to jobId,
                "workerId" to (jobSnapshot.child("workerId").getValue(String::class.java) ?: ""),
                "customerId" to (jobSnapshot.child("customerId").getValue(String::class.java) ?: ""),
                "serviceName" to (jobSnapshot.child("serviceName").getValue(String::class.java) ?: ""),
                "price" to (jobSnapshot.child("price").getValue(Int::class.java) ?: 0),
                "completedAt" to timestamp,
                "scheduledDate" to (jobSnapshot.child("scheduledDate").getValue(Long::class.java) ?: timestamp),
                "status" to "COMPLETED"
            )
            jobHistoryCollection.add(jobHistory).await()
            
            // Update Firestore job document
            jobsCollection.document(jobId).update(
                mapOf(
                    "status" to "COMPLETED",
                    "completedAt" to timestamp,
                    "updatedAt" to timestamp
                )
            ).await()
            
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
            val timestamp = System.currentTimeMillis()
            
            // Realtime Database
            val realtimeWorker = hashMapOf(
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
            workersRef.child(userId).setValue(realtimeWorker).await()
            
            // Firestore
            val firestoreWorker = hashMapOf(
                "id" to userId,
                "name" to name,
                "phone" to phone,
                "email" to "",
                "completedJobs" to 0,
                "averageRating" to 0.0,
                "totalEarnings" to 0,
                "todayEarnings" to 0,
                "status" to WorkerStatus.PENDING.name,
                "specialties" to emptyList<String>(),
                "createdAt" to timestamp,
                "updatedAt" to timestamp
            )
            workersCollection.document(userId).set(firestoreWorker).await()
            
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
    
    // ============ FIRESTORE METHODS - Analytics & Complex Queries ============
    
    // Lấy lịch sử công việc của worker (từ Firestore)
    suspend fun getWorkerJobHistory(
        workerId: String,
        limit: Int = 50
    ): Result<List<JobHistoryItem>> {
        return try {
            val snapshot = jobHistoryCollection
                .whereEqualTo("workerId", workerId)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val history = snapshot.documents.mapNotNull { doc ->
                try {
                    JobHistoryItem(
                        jobId = doc.getString("jobId") ?: "",
                        serviceName = doc.getString("serviceName") ?: "",
                        price = doc.getLong("price")?.toInt() ?: 0,
                        completedAt = doc.getLong("completedAt") ?: 0L,
                        scheduledDate = doc.getLong("scheduledDate") ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Thống kê thu nhập theo ngày (từ Firestore)
    suspend fun getEarningsStatistics(
        workerId: String,
        startDate: Long,
        endDate: Long
    ): Result<EarningsStats> {
        return try {
            val snapshot = jobHistoryCollection
                .whereEqualTo("workerId", workerId)
                .whereGreaterThanOrEqualTo("completedAt", startDate)
                .whereLessThanOrEqualTo("completedAt", endDate)
                .get()
                .await()
            
            var totalEarnings = 0
            var totalJobs = 0
            
            snapshot.documents.forEach { doc ->
                totalEarnings += doc.getLong("price")?.toInt() ?: 0
                totalJobs++
            }
            
            val stats = EarningsStats(
                totalEarnings = totalEarnings,
                totalJobs = totalJobs,
                averagePerJob = if (totalJobs > 0) totalEarnings / totalJobs else 0,
                period = "custom"
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Tìm kiếm workers theo specialties (từ Firestore)
    suspend fun searchWorkersBySpecialty(specialty: String): Result<List<Worker>> {
        return try {
            val snapshot = workersCollection
                .whereArrayContains("specialties", specialty)
                .whereEqualTo("status", WorkerStatus.APPROVED.name)
                .get()
                .await()
            
            val workers = snapshot.documents.mapNotNull { doc ->
                try {
                    Worker(
                        id = doc.getString("id") ?: "",
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        completedJobs = doc.getLong("completedJobs")?.toInt() ?: 0,
                        averageRating = doc.getDouble("averageRating") ?: 0.0,
                        totalEarnings = doc.getLong("totalEarnings")?.toInt() ?: 0,
                        todayEarnings = doc.getLong("todayEarnings")?.toInt() ?: 0,
                        specialties = (doc.get("specialties") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        status = WorkerStatus.valueOf(doc.getString("status") ?: "PENDING"),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(workers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Lấy top workers theo rating (từ Firestore)
    suspend fun getTopWorkers(limit: Int = 10): Result<List<Worker>> {
        return try {
            val snapshot = workersCollection
                .whereEqualTo("status", WorkerStatus.APPROVED.name)
                .orderBy("averageRating", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val workers = snapshot.documents.mapNotNull { doc ->
                try {
                    Worker(
                        id = doc.getString("id") ?: "",
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        completedJobs = doc.getLong("completedJobs")?.toInt() ?: 0,
                        averageRating = doc.getDouble("averageRating") ?: 0.0,
                        totalEarnings = doc.getLong("totalEarnings")?.toInt() ?: 0,
                        todayEarnings = doc.getLong("todayEarnings")?.toInt() ?: 0,
                        specialties = (doc.get("specialties") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        status = WorkerStatus.valueOf(doc.getString("status") ?: "PENDING"),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(workers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Lưu analytics event (từ Firestore)
    suspend fun logAnalyticsEvent(
        eventType: String,
        userId: String,
        data: Map<String, Any>
    ): Result<Unit> {
        return try {
            val event = hashMapOf(
                "eventType" to eventType,
                "userId" to userId,
                "timestamp" to System.currentTimeMillis(),
                "data" to data
            )
            
            analyticsCollection.add(event).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
