package com.example.donvesinhcuanv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donvesinhcuanv.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobViewModel : ViewModel() {
    private val firebaseRepository = if (AppConfig.USE_FIREBASE) FirebaseRepository() else null
    private val offlineRepository = if (!AppConfig.USE_FIREBASE) OfflineRepository() else null
    
    private val _pendingJobs = MutableStateFlow<List<Job>>(emptyList())
    val pendingJobs: StateFlow<List<Job>> = _pendingJobs.asStateFlow()
    
    private val _acceptedJobs = MutableStateFlow<List<Job>>(emptyList())
    val acceptedJobs: StateFlow<List<Job>> = _acceptedJobs.asStateFlow()
    
    private val _completedJobs = MutableStateFlow<List<Job>>(emptyList())
    val completedJobs: StateFlow<List<Job>> = _completedJobs.asStateFlow()
    
    private val _currentWorker = MutableStateFlow(SampleData.currentWorker)
    val currentWorker: StateFlow<Worker> = _currentWorker.asStateFlow()
    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadWorkerData()
        observePendingJobs()
    }
    
    // Public method to refresh worker data
    fun refreshWorkerData() {
        loadWorkerData()
    }
    
    private fun loadWorkerData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            if (AppConfig.USE_FIREBASE) {
                val userId = firebaseRepository?.getCurrentUser()?.uid
                if (userId != null) {
                    println("🔍 Loading worker data for userId: $userId")
                    val result = firebaseRepository.getWorker(userId)
                    if (result.isSuccess) {
                        val worker = result.getOrThrow()
                        println("✅ Worker loaded: name=${worker.name}, status=${worker.status}")
                        _currentWorker.value = worker
                        _isOnline.value = worker.isOnline
                        observeWorkerJobs(userId)
                    } else {
                        println("❌ Failed to load worker: ${result.exceptionOrNull()?.message}")
                    }
                }
            } else {
                val worker = offlineRepository?.getCurrentUser() ?: SampleData.currentWorker
                _currentWorker.value = worker
                _isOnline.value = worker.isOnline
                observeWorkerJobs(worker.id)
            }
            
            _isLoading.value = false
        }
    }
    
    private fun observePendingJobs() {
        viewModelScope.launch {
            if (AppConfig.USE_FIREBASE) {
                firebaseRepository?.getPendingJobs()?.collect { jobs ->
                    _pendingJobs.value = jobs
                }
            } else {
                offlineRepository?.getPendingJobs()?.collect { jobs ->
                    _pendingJobs.value = jobs
                }
            }
        }
    }
    
    private fun observeWorkerJobs(workerId: String) {
        viewModelScope.launch {
            if (AppConfig.USE_FIREBASE) {
                firebaseRepository?.getWorkerJobs(workerId)?.collect { jobs ->
                    _acceptedJobs.value = jobs
                }
            } else {
                offlineRepository?.getWorkerJobs(workerId)?.collect { jobs ->
                    _acceptedJobs.value = jobs
                }
            }
        }
    }
    
    fun toggleOnlineStatus() {
        viewModelScope.launch {
            val newStatus = !_isOnline.value
            _isOnline.value = newStatus
            
            if (AppConfig.USE_FIREBASE) {
                val userId = firebaseRepository?.getCurrentUser()?.uid
                if (userId != null) {
                    firebaseRepository.updateWorkerOnlineStatus(userId, newStatus)
                }
            } else {
                val userId = offlineRepository?.getCurrentUser()?.id
                if (userId != null) {
                    offlineRepository?.updateWorkerOnlineStatus(userId, newStatus)
                }
            }
            
            _currentWorker.value = _currentWorker.value.copy(isOnline = newStatus)
        }
    }
    
    fun acceptJob(job: Job) {
        viewModelScope.launch {
            if (AppConfig.USE_FIREBASE) {
                val userId = firebaseRepository?.getCurrentUser()?.uid
                if (userId != null) {
                    firebaseRepository.acceptJob(job.id, userId)
                }
            } else {
                val userId = offlineRepository?.getCurrentUser()?.id
                if (userId != null) {
                    offlineRepository?.acceptJob(job.id, userId)
                }
            }
        }
    }
    
    fun startArriving(job: Job) {
        viewModelScope.launch {
            if (AppConfig.USE_FIREBASE) {
                firebaseRepository?.updateJobStatus(job.id, JobStatus.ARRIVING)
            } else {
                offlineRepository?.updateJobStatus(job.id, JobStatus.ARRIVING)
            }
        }
    }
    
    fun startJob(job: Job) {
        viewModelScope.launch {
            if (AppConfig.USE_FIREBASE) {
                firebaseRepository?.updateJobStatus(job.id, JobStatus.IN_PROGRESS)
            } else {
                offlineRepository?.updateJobStatus(job.id, JobStatus.IN_PROGRESS)
            }
        }
    }
    
    fun completeJob(job: Job) {
        viewModelScope.launch {
            if (AppConfig.USE_FIREBASE) {
                val result = firebaseRepository?.completeJob(job.id)
                if (result?.isSuccess == true) {
                    val userId = firebaseRepository.getCurrentUser()?.uid
                    if (userId != null) {
                        firebaseRepository.updateWorkerEarnings(userId, job.price)
                        loadWorkerData()
                    }
                }
            } else {
                offlineRepository?.completeJob(job.id)
                val userId = offlineRepository?.getCurrentUser()?.id
                if (userId != null) {
                    offlineRepository.updateWorkerEarnings(userId, job.price)
                    loadWorkerData()
                }
            }
        }
    }
    
    fun cancelJob(job: Job) {
        viewModelScope.launch {
            if (AppConfig.USE_FIREBASE) {
                firebaseRepository?.updateJobStatus(job.id, JobStatus.PENDING)
            } else {
                offlineRepository?.updateJobStatus(job.id, JobStatus.PENDING)
            }
        }
    }
}
