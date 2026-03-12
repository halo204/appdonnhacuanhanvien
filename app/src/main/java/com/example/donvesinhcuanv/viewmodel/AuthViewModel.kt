package com.example.donvesinhcuanv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donvesinhcuanv.data.MySQLRepository
import com.example.donvesinhcuanv.data.WorkerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = MySQLRepository()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        _isLoggedIn.value = repository.isLoggedIn()
    }
    
    fun signUp(email: String, password: String, fullName: String, phone: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = repository.signUp(email, password, fullName, phone)
            
            _authState.value = if (result.isSuccess) {
                _isLoggedIn.value = true
                val worker = result.getOrThrow()
                AuthState.Success(worker.id)
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Đăng ký thất bại")
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = repository.signIn(email, password)
            
            _authState.value = if (result.isSuccess) {
                _isLoggedIn.value = true
                val worker = result.getOrThrow()
                AuthState.Success(worker.id)
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Đăng nhập thất bại")
            }
        }
    }
    
    fun signOut() {
        repository.signOut()
        _isLoggedIn.value = false
        _authState.value = AuthState.Idle
    }
    
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
