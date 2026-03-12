package com.example.donvesinhcuanv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donvesinhcuanv.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobViewModel : ViewModel() {
    private val repository = MySQLRepository()
    
    private val _pendingJobs = MutableStateFlow<List<Job>>(emptyList())
    val pendingJobs: StateFlow<List<Job>> = _pendingJobs.asStateFlow()
    
    private val _acceptedJobs = MutableStateFlow<List<Job>>(emptyList())
    val acceptedJobs: StateFlow<List<Job>> = _acceptedJobs.asStateFlow()
    
    private val _currentWorker = MutableStateFlow<Worker?>(null)
    val currentWorker: StateFlow<Worker?> = _currentWorker.asStateFlow()
    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadWorkerData()
        observePendingJobs()
    }
    
    fun refreshWorkerData() {
        loadWorkerData()
    }
    
    private fun loadWorkerData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val worker = repository.getCurrentWorker()
            if (worker != null) {
                _currentWorker.value = worker
                _isOnline.value = worker.isOnline
                observeWorkerJobs(worker.id)
            }
            
            _isLoading.value = false
        }
    }
    
    private fun observePendingJobs() {
        viewModelScope.launch {
            repository.getPendingJobs().collect { jobs ->
                _pendingJobs.value = jobs
            }
        }
    }
    
    private fun observeWorkerJobs(workerId: String) {
        viewModelScope.launch {
            repository.getWorkerJobs(workerId).collect { jobs ->
                _acceptedJobs.value = jobs
            }
        }
    }
    
    fun toggleOnlineStatus() {
        viewModelScope.launch {
            val worker = _currentWorker.value ?: return@launch
            val newStatus = !_isOnline.value
            
            repository.updateWorkerOnlineStatus(worker.id, newStatus)
            _isOnline.value = newStatus
            _currentWorker.value = worker.copy(isOnline = newStatus)
        }
    }
    
    fun acceptJob(job: Job) {
        viewModelScope.launch {
            val worker = _currentWorker.value ?: return@launch
            repository.acceptJob(job.id, worker.id)
            // Refresh jobs
            observePendingJobs()
            observeWorkerJobs(worker.id)
        }
    }
    
    fun startArriving(job: Job) {
        viewModelScope.launch {
            repository.updateJobStatus(job.id, JobStatus.ARRIVING)
        }
    }
    
    fun startJob(job: Job) {
        viewModelScope.launch {
            repository.updateJobStatus(job.id, JobStatus.IN_PROGRESS)
        }
    }
    
    fun completeJob(job: Job) {
        viewModelScope.launch {
            val result = repository.completeJob(job.id)
            if (result.isSuccess) {
                loadWorkerData()
            }
        }
    }
    
    fun cancelJob(job: Job) {
        viewModelScope.launch {
            repository.updateJobStatus(job.id, JobStatus.PENDING)
        }
    }
}
